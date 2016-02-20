(ns ^:no-doc structural-typing.guts.compile.to-specter-path
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

(defn no-transform! []
  (boom! "structural-typing does not use transform"))


;; NOTE: Specter requires `extend-type/extend-protocol` instead of
;; defining the protocol functions in the deftype. It's an
;; implementation detail.

(defrecord KeywordVariantType [keyword])


(defn- associative-select* [accessor this structure next-fn]
  (cond (map? structure)
        (next-fn (get structure (accessor this)))

        ;; This code could return `nil` immediately, rather than calling `next-fn`.
        ;; That would (I think) provide an easier way of handling cases like this:
        ;;    (built-like {[:k ALL] required-path} {}) => (just (err:required :k))
        ;; ... than the current method, which relies on `add-implied-required-paths`.
        ;; However, that code was already added back when I was using Specter's
        ;; extension of `clojure.lang.Keyword` rather than rolling my own. It's easier
        ;; to keep it than take it out.
        ;;
        ;; Also this would allow something like `{[ALL :k some-VERY-peculiar-predicate] ...}`
        ;; to do something like, oh, replacing the nth `nil` with its count.
        (nil? structure)
        (next-fn nil)

        :else
        (boom! "%s is not a map" structure)))

(extend-type KeywordVariantType
  sp/StructurePath
  (select* [& args] (apply associative-select* :keyword args))
  (transform* [& _] (no-transform!)))

(defmethod clojure.core/print-method KeywordVariantType [o, ^java.io.Writer w]
  (.write w (str (.-keyword o))))

;;
(defrecord StringVariantType [string])

(extend-type StringVariantType
  sp/StructurePath
  (select* [& args] (apply associative-select* :string args))
  (transform* [& _] (no-transform!)))

(defmethod clojure.core/print-method StringVariantType [o, ^java.io.Writer w]
  (.write w (pr-str (.-string o))))

;;
(deftype IntegerVariantType [value])
  
(extend-type IntegerVariantType
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
  (transform* [& _] (no-transform!)))

(defmethod clojure.core/print-method IntegerVariantType [o, ^java.io.Writer w]
  (.write w (str (.-value o))))




                                 ;;; ALL, RANGE, etc.

(defn pursue-multiple-paths [subcollection-fn collection next-fn]
  (into [] (r/mapcat next-fn (subcollection-fn collection))))


;;; ALL
(deftype AllVariantType [])

(extend-type AllVariantType
  sp/StructurePath
  (select* [this structure next-fn]
    (into [] (r/mapcat next-fn structure)))
  (transform* [& _] (no-transform!)))

(def ALL (->AllVariantType))

(defmethod clojure.core/print-method AllVariantType [o, ^java.io.Writer w] (.write w "ALL"))
(readable/instead-of ALL 'ALL)



;;; RANGE
(defn desired-range [{:keys [inclusive-start exclusive-end]} sequence]
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
    result))

(defrecord RangeVariantType [inclusive-start exclusive-end])

(extend-type RangeVariantType
  sp/StructurePath
  (select* [this structure next-fn]
    (into [] (r/mapcat next-fn (desired-range this structure))))
  (transform* [& _] (no-transform!)))

(defn RANGE
  "Use this in a path to select a range of values in a 
   collection. The first argument is inclusive; the second exclusive.
   
       (type! :ELEMENTS-1-AND-2-ARE-EVEN {[(RANGE 1 3)] even?})
"
  [inclusive-start exclusive-end]
  (->RangeVariantType inclusive-start exclusive-end))


(defmethod clojure.core/print-method RangeVariantType [o, ^java.io.Writer w]
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
  (transform* [& _] (no-transform!)))

(def ONLY (->OnlyVariantType))

(defmethod clojure.core/print-method OnlyVariantType [o, ^java.io.Writer w] (.write w "ONLY"))
(readable/instead-of ONLY 'ONLY)



;;;;; 


(defn will-match-many? [elt]
  (or (#{ALL} elt)
      (instance? RangeVariantType elt)))

;; A pseudo-predicate to short-circuit processing with an error when a non-sequential is
;; to be given to RANGE. Note: although `nil` is actually non-sequential, it is allowed
;; because it typically represents a too-short sequence, which should get a different error.
(defn- range-requires-sequential! [x]
  (when (and (not (sequential? x))
             (not (nil? x)))
    (throw+ {:type :bad-range-target :interior-node x}))
  true)

(defn- short-circuit-on-nil [x]
  (not (nil? x)))

(defn- all-requires-collection! [x]  ;; assumes nil has already been filtered out.
  (when (or (map? x)
            (not (coll? x)))
    (throw+ {:type :bad-all-target :interior-node x}))
  true)


(defn- surround-with-index-collector [elt]
  (vector (specter/view #(map-indexed vector %))
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
                  (into [short-circuit-on-nil all-requires-collection!]
                        (surround-with-index-collector elt))

                  (instance? RangeVariantType elt)
                  (into [range-requires-sequential!]
                        (surround-with-index-collector elt))

                  (keyword? elt)
                  (prefix-with-elt-collector elt (->KeywordVariantType elt))

                  (string? elt)
                  (prefix-with-elt-collector elt (->StringVariantType elt))

                  (integer? elt)
                  (prefix-with-elt-collector elt (->IntegerVariantType elt))

                  :else
                  (prefix-with-elt-collector elt elt))]
        (recur remainder (into specter-path new-path))))))

(defn compile [original-path]
  (if (empty? original-path)
    (fn [whole-value] (vector (exval/->ExVal whole-value [] whole-value)))
    (let [compiled-path (apply specter/comp-paths (munge-path-appropriately original-path))]
      (fn [whole-value]
        (let [result (specter/compiled-select compiled-path whole-value)]
          (mapv #(exval/->ExVal (last %) (butlast %) whole-value) result))))))

(defn mkfn:whole-value->oopsies [original-path lifted-preds]
  (let [exval-maker (compile original-path)]
    (fn [whole-value]
      (try+
       (mapcat lifted-preds (exval-maker whole-value))

       (catch [:type :bad-range-target] {:keys [interior-node]}
          (explain/as-oopsies:bad-range-target original-path whole-value interior-node))

        (catch [:type :bad-all-target] {:keys [interior-node]}
          (explain/as-oopsies:bad-all-target original-path whole-value interior-node))

        (catch [:type :bad-range-target] {:keys [interior-node]}
          (explain/as-oopsies:bad-range-target original-path whole-value interior-node))

        (catch [:type :only] {:keys [interior-node]}
          (explain/as-oopsies:only original-path whole-value interior-node))

        (catch Exception ex
          (explain/as-oopsies:notpath original-path whole-value))))))
