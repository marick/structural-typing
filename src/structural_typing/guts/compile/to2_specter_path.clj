(ns ^:no-doc structural-typing.guts.compile.to2-specter-path
  (:use structural-typing.clojure.core)
  (:refer-clojure :exclude [compile])
  (:require [com.rpl.specter :as specter]
            [com.rpl.specter.protocols :as sp]
            [clojure.core.reducers :as r]
            [such.readable :as readable]
            [structural-typing.guts.self-check :as self :refer [returns-many]]
            [structural-typing.guts.explanations :as explain]
            [structural-typing.guts.exval :as exval]
            [structural-typing.guts.expred :as expred]
            [structural-typing.guts.preds.wrap :as wrap]
            [structural-typing.assist.oopsie :as oopsie]
            [slingshot.slingshot :refer [throw+ try+]]))


;; NOTE: Specter requires `extend-type/extend-protocol` instead of
;; defining the protocol functions in the deftype. It's an
;; implementation detail.

(deftype KeywordVariantType222 [keyword])

(extend-type KeywordVariantType222
  sp/StructurePath
  (select* [this structure next-fn]
    (cond (map? structure)
          (next-fn (get structure (.-keyword this)))

          (nil? structure)
          (next-fn nil)

          :else
          (boom! "%s is not a map" structure)))
  (transform* [this structure next-fn] (boom! "structural-typing does not use transform")))

(defmethod clojure.core/print-method KeywordVariantType222 [o, ^java.io.Writer w]
  (.write w (str (.-keyword o))))

;;
(deftype StringVariantType222 [string])

(extend-type StringVariantType222
  sp/StructurePath
  (select* [this structure next-fn]
    (cond (map? structure)
          (next-fn (get structure (.-string this)))

          (nil? structure)
          (next-fn nil)

          :else
          (boom! "%s is not a map" structure)))
  (transform* [this structure next-fn] (boom! "structural-typing does not use transform")))

(defmethod clojure.core/print-method StringVariantType222 [o, ^java.io.Writer w]
  (.write w (pr-str (.-string o))))

;;
(deftype IntegerVariantType222 [value])
  
(extend-type IntegerVariantType222
  sp/StructurePath
  (select* [this structure next-fn]
    (cond (nil? structure)
          (next-fn nil)
          
          (not (sequential? structure))
          (boom! "%s is not sequential" structure)

          :else
          (try+
            (next-fn (nth structure (.-value this)))
            (catch IndexOutOfBoundsException ex
              (next-fn nil)))))
  (transform* [kw structure next-fn] (boom! "structural-typing does not use transform")))

(defmethod clojure.core/print-method IntegerVariantType222 [o, ^java.io.Writer w]
  (.write w (str (.-value o))))




                                 ;;; ALL, RANGE, etc.

(defn pursue-multiple-paths [subcollection-fn collection next-fn]
  (cond (nil? collection)
        nil

        :else
        (into [] (r/mapcat next-fn (subcollection-fn collection)))))


;;; ALL
(deftype AllVariantType222 [])

(extend-type AllVariantType222
  sp/StructurePath
  (select* [this structure next-fn] (pursue-multiple-paths identity structure next-fn))
  (transform* [kw structure next-fn] (boom! "structural-typing does not use transform")))

(def ALL (->AllVariantType222))

(defmethod clojure.core/print-method AllVariantType222 [o, ^java.io.Writer w] (.write w "ALL"))
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

(defrecord RangeVariantType222 [inclusive-start exclusive-end])

(extend-type RangeVariantType222
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
  (->RangeVariantType222 inclusive-start exclusive-end))


(defmethod clojure.core/print-method RangeVariantType222 [o, ^java.io.Writer w]
  (.write w (format "(RANGE %s %s)" (:inclusive-start o) (:exclusive-end o))))



;;; ONLY
(deftype OnlyVariantType [])

(extend-type OnlyVariantType
  sp/StructurePath
  (select* [this structure next-fn]
    (cond (not (coll? structure))
          (boom! "%s is not a collection" structure)

          (not= 1 (count structure))
          (throw+ {:type :only, :interior-node structure})

          :else
          (next-fn (first structure))))

  (transform* [kw structure next-fn] (boom! "structural-typing does not use transform")))

(def ONLY (->OnlyVariantType))

(defmethod clojure.core/print-method OnlyVariantType [o, ^java.io.Writer w] (.write w "ONLY"))
(readable/instead-of ONLY 'ONLY)



;;;;; 


(defn will-match-many? [elt]
  (or (#{ALL} elt)
      (instance? RangeVariantType222 elt)))

;; A pseudo-predicate to short-circuit processing with an error when a non-sequential is
;; to be given to RANGE. Note: although `nil` is actually non-sequential, it is allowed
;; because it typically represents a too-short sequence, which should get a different error.
(defn- range-requires-sequential! [x]
  (when (and (not (sequential? x))
             (not (nil? x)))
    (throw+ {:type :bad-range-target :interior-node x}))
  true)

(defn- all-may-not-be-nil! [x]
  (when (nil? x)
    (throw+ {:type :nil-all}))
  true)

(defn- all-requires-collection! [x]
  (when (or (map? x)
            (and (not (nil? x))
                 (not (coll? x))))
    (throw+ {:type :bad-all-target :interior-node x}))
  true)


(defn- surround-with-index-collector [elt]
  (vector (specter/view #(into (empty %) (map-indexed vector %)))
          elt
          (specter/collect-one specter/FIRST)
          specter/LAST))

(defn- prefix-with-elt-collector [original-elt dispatch-version-of-elt]
  (vector (specter/putval original-elt) dispatch-version-of-elt))


(defn- munge-path-appropriately [original-path]
  (loop [[elt & remainder] original-path
         specter-path []]
    (if (nil? elt)
      specter-path
      (let [new-path
            (cond (= ALL elt)
                  (surround-with-index-collector elt)

                  (instance? RangeVariantType222 elt)
                  (surround-with-index-collector elt)

                  (keyword? elt)
                  (prefix-with-elt-collector elt (->KeywordVariantType222 elt))

                  (string? elt)
                  (prefix-with-elt-collector elt (->StringVariantType222 elt))

                  (integer? elt)
                  (prefix-with-elt-collector elt (->IntegerVariantType222 elt))

                  :else
                  (prefix-with-elt-collector elt elt))]
        (recur remainder (into specter-path new-path))))))

(defn compile [original-path]
  (if (empty? original-path)
    (fn [whole-value] (exval/->ExVal whole-value [] whole-value))
    (let [compiled-path (apply specter/comp-paths (munge-path-appropriately original-path))]
      (fn [whole-value]
        (let [result (specter/compiled-select compiled-path whole-value)]
          (mapv #(exval/->ExVal (last %) (butlast %) whole-value) result))))))

(defn mkfn:whole-value->oopsies [original-path lifted-preds]
  (let [exval-maker (compile original-path)]
    (fn [whole-value]
      (try+
       (for [exval (exval-maker whole-value)
             raw-oopsie (lifted-preds exval)]
         raw-oopsie)

        (catch [:type :bad-range-target] {:keys [interior-node]}
          (explain/as-oopsies:bad-range-target original-path whole-value interior-node))

        (catch [:type :bad-all-target] {:keys [interior-node]}
          (explain/as-oopsies:bad-all-target original-path whole-value interior-node))

        (catch [:type :bad-range-target] {:keys [interior-node]}
          (explain/as-oopsies:bad-range-target original-path whole-value interior-node))

        (catch [:type :nil-all] {}
          (explain/as-oopsies:nil-all original-path whole-value))

        (catch [:type :only] {:keys [interior-node]}
          (explain/as-oopsies:only original-path whole-value interior-node))

        (catch Exception ex
          (explain/as-oopsies:notpath original-path whole-value))))))
