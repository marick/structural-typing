(ns ^:no-doc structural-typing.guts.compile.to-specter-path
  (:use structural-typing.clojure.core)
  (:refer-clojure :exclude [compile])
  (:require [com.rpl.specter :as specter]
            [com.rpl.specter.protocols :as sp]
            [clojure.core.reducers :as r]
            [such.readable :as readable]
            [structural-typing.guts.self-check :as self :refer [returns-many]]
            [structural-typing.guts.exval :as exval]
            [structural-typing.guts.expred :as expred]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.guts.preds.wrap :as wrap]))

(extend-type clojure.lang.Keyword
  sp/StructurePath
  (select* [kw structure next-fn]
    (cond (map? structure)
          (next-fn (get structure kw))

          (nil? structure)
          (next-fn nil)

          :else
          (boom! "%s is not a map" structure)))
  (transform* [kw structure next-fn] (boom! "structural-typing does not use transform")))

(extend-type java.lang.Long
  sp/StructurePath
  (select* [this structure next-fn] 
    (cond (nil? structure)
          (next-fn nil)
          
          (not (sequential? structure))
          (boom! "%s is not sequential" structure)

          :else
          (try
            (next-fn (nth structure this))
            (catch IndexOutOfBoundsException ex
              (next-fn nil)))))
  (transform* [kw structure next-fn] (boom! "structural-typing does not use transform")))


                                 ;;; ALL, RANGE

(defn pursue-multiple-paths [subcollection-fn collection next-fn]
  (cond (nil? collection)
        (next-fn nil)

        (not (coll? collection))
        (boom! "%s is not a collection" collection)

        :else
        (into [] (r/mapcat next-fn (subcollection-fn collection)))))

;;; ALL
(deftype AllVariantType [])

(extend-type AllVariantType
  sp/StructurePath
  (select* [this structure next-fn] (pursue-multiple-paths identity structure next-fn))
  (transform* [kw structure next-fn] (boom! "structural-typing does not use transform")))

(def ALL (->AllVariantType))

(defmethod clojure.core/print-method AllVariantType [o, ^java.io.Writer w] (.write w "ALL"))
(readable/instead-of ALL 'ALL)

;;; RANGE
(defn mkfn:range-element-selector [{:keys [inclusive-start exclusive-end]}]
  (fn [sequence]
    (let [desired-count (- exclusive-end inclusive-start)
          subseq (->> sequence
                      (drop inclusive-start)
                      (take desired-count)
                      vec)
          actual-count (count subseq)
          result (if (= actual-count desired-count)
                   subseq
                   (into subseq
                         (map vector 
                              (drop (+ inclusive-start actual-count) (range))
                              (repeat (- desired-count actual-count) nil))))]
      result)))

(defrecord RangeVariantType [inclusive-start exclusive-end])

(extend-type RangeVariantType
  sp/StructurePath
  (select* [this structure next-fn]
    (if (or (map? structure) (set? structure))
      (boom! "Cannot take a map or a set")
      (pursue-multiple-paths (mkfn:range-element-selector this) structure next-fn)))
  (transform* [kw structure next-fn] (boom! "structural-typing does not use transform")))

(defn RANGE
  "Use this in a path to select a range of values in a 
   collection. The first argument is inclusive; the second exclusive.
   
       (type! :ELEMENTS-1-AND-2-ARE-EVEN {[(RANGE 1 3)] even?})
"
  [inclusive-start exclusive-end]
  (->RangeVariantType inclusive-start exclusive-end))


(defmethod clojure.core/print-method RangeVariantType [o, ^java.io.Writer w]
  (.write w (format "(RANGE %s %s)" (:inclusive-start o) (:exclusive-end o))))




;;;;; 


(defn will-match-many? [elt]
  (or (= elt ALL)
      (instance? RangeVariantType elt)))

(defn replace-with-indices [path indices]
  (loop [result []
         [p & ps] path
         indices indices]
    (cond (nil? p)
          result

          (will-match-many? p)
          (recur (conj result (first indices))
                 ps
                 (rest indices))

          :else
          (recur (conj result p)
                 ps
                 indices))))

(defn- surround-with-index-collector [elt]
  (-> [(specter/view (partial map-indexed vector))]
      (into (vector elt))
      (into [(specter/collect-one specter/FIRST)
             specter/LAST])))

(defn compile [original-path]
   (loop [[elt & remainder] original-path
          specter-path []
          path-type :constant-path]
     (cond (nil? elt)
           [(apply specter/comp-paths specter-path) path-type]

           (will-match-many? elt)
           (recur remainder
                  (into specter-path
                        (surround-with-index-collector elt))
                  :indexed-path)

           :else
           (recur remainder (conj specter-path elt) path-type))))

(def constant-path-exval-maker exval/->ExVal)
(defn constant-path-postprocessor [specter-result oopsie] oopsie)
(defn indexed-path-exval-maker [specter-result original-path whole-value]
  (exval/->ExVal (last specter-result) original-path whole-value))
(defn indexed-path-postprocessor [specter-result raw-oopsie]
  (update raw-oopsie :path #(replace-with-indices % (butlast specter-result))))

(defn processors [path-type]
  (case path-type
    :constant-path [constant-path-exval-maker constant-path-postprocessor]
    :indexed-path [indexed-path-exval-maker indexed-path-postprocessor]
    (boom! "%s is an invalid path-type (neither constant nor indexed)" path-type)))

(defn- impossible-path-oopsie [original-path whole-value]
  (merge (expred/->ExPred 'impossible-path
                          "check for impossible path"
                          (constantly (format "%s is not a path into `%s`"
                                              (oopsie/friendly-path {:path original-path})
                                              (pr-str whole-value))))
         (exval/->ExVal :no-leaf
                        whole-value
                        original-path)))
  

(defn mkfn:whole-value->oopsies [original-path lifted-preds]
  (let [[compiled-path path-type] (compile original-path)
        [exval-maker path-postprocessor] (processors path-type)]
    (fn [whole-value]
      (try
        (let [specter-results (specter/compiled-select compiled-path whole-value)]
          (for [result specter-results
                raw-oopsie (lifted-preds (exval-maker result original-path whole-value))]
            (path-postprocessor result raw-oopsie)))
        (catch Exception ex
          (vector (impossible-path-oopsie original-path whole-value)))))))
