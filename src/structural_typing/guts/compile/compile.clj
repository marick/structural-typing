(ns ^:no-doc structural-typing.guts.compile.compile
  (:use structural-typing.clojure.core)
  (:require [com.rpl.specter :as specter]
            [structural-typing.guts.self-check :as self :refer [returns-many]]
            [structural-typing.guts.type-descriptions.elements :as element]
            [structural-typing.guts.oopsie :as oopsie]
            [structural-typing.guts.preds.wrap :as wrap]))


(defn path-will-match-many? [path]
  (boolean (some element/will-match-many? path)))

(defn index-collecting-splice [elt]
  (let [note-index (specter/view (partial map-indexed vector)) ; value [x y] -> [ [0 x] [1 y] ]
                                                               ; for next step
        specific (element/specter-equivalent elt)  ; most often, this will splice in `ALL`
        prepend-index (specter/collect-one specter/FIRST)  ; stash the index (0 or 1 above) so that
                                                           ; Specter will prepend to final result.
        intermediate-value specter/LAST]  ; Further selectors apply to the original val (x and y)
    ;; Typical example:
    ;;    Path: [:x ALL even?]
    ;;    Input: [ {:x 100} {:x 101} {:x 102} ]
    ;;    Result [ [0 100]           [2 102] ]

    (-> [note-index]
        (into specific)
        (conj prepend-index)
        (conj intermediate-value))))

(def force-collection-of-indices
  (lazyseq:x->abc index-collecting-splice element/will-match-many?))

(defn replace-with-indices [path indices]
  (loop [result []
         [p & ps] path
         indices indices]
    (cond (nil? p)
          result

          (element/will-match-many? p)
          (recur (conj result (first indices))
                 ps
                 (rest indices))

          :else
          (recur (conj result p)
                 ps
                 indices))))




(defprotocol PathVariation
  (process-specter-results [this building-results])
  (spread-leaf-values [this building-results])
  (adjust-path [this oopsie-superset])
  )

(defrecord AllSingleSelectorVariation [original-path compiled-path compiled-preds]
  PathVariation
  (process-specter-results [this building-results]
    (assoc building-results :leaf-values (:specter-results building-results)))

  (spread-leaf-values [variation building-results]
    (map #(assoc building-results :leaf-value %) (:leaf-values building-results)))

  (adjust-path [this oopsie-superset]
    oopsie-superset))

(defrecord WildcardVariation [original-path compiled-path compiled-preds]
  PathVariation
  (process-specter-results [this building-results]
    (assoc building-results
           :leaf-values (map last (:specter-results building-results))
           :path-adjustments (map butlast (:specter-results building-results))))

  (spread-leaf-values [variation building-results]
    (map #(assoc building-results :leaf-value %1 :path-adjustment %2)
         (:leaf-values building-results) (:path-adjustments building-results)))
       
  (adjust-path [this {:keys [:path :path-adjustment] :as oopsie-superset}]
    (assoc oopsie-superset
           :path (replace-with-indices path path-adjustment)
           :specter-path path))
           
)

(defn compile-predicates [preds]
  (let [lifted (map wrap/lift preds)]
    (fn [value-holder]
      (->> (reduce #(into %1 (%2 value-holder)) [] lifted)
           (returns-many :expred)))))


(defn run-preds [variation building-values]
  (->> building-values
       (mapcat (:compiled-preds variation))
       (map #(adjust-path variation %))))

(defn run-specter [variation whole-value]
  (->> {:path (:original-path variation)
        :whole-value whole-value
        :specter-results (specter/compiled-select (:compiled-path variation) whole-value)}
       (process-specter-results variation)))


(defn capture-path-variation [original-path preds]
  (let [match-many? (path-will-match-many? original-path)
        path-adjustment (if match-many? force-collection-of-indices identity)
        compiled-path (apply specter/comp-paths (path-adjustment original-path))
        compiled-preds (compile-predicates preds)
        constructor (if match-many? ->WildcardVariation ->AllSingleSelectorVariation)]
    (constructor original-path compiled-path compiled-preds)))

(defn compile-path-check [[original-path preds]]
  (let [variation (capture-path-variation original-path preds)]
    (fn [whole-value]
      (try 
        (->> whole-value
             (run-specter variation)
             (process-specter-results variation)
             (spread-leaf-values variation)        (returns-many :exval)
             (run-preds variation)                 (returns-many :oopsie))
        (catch Exception ex
          (vector {:explainer (constantly (format "%s is not a path into `%s` - note: this can happen if you used a `includes` inside an `implies` predicate - sorry I can't give a better error message - complain to marick@exampler.com if you see this."
                                                  (oopsie/friendly-path {:path original-path})
                                                  (pr-str whole-value)))
                   ;; These are just for debugging should it be needed.
                   :whole-value whole-value
                   :path original-path}))))))

(defn compile-type [type-map]
  ;; Note that the path-checks are compiled once, returning a function to be run often.
  (let [compiled-path-checks (map compile-path-check type-map)]
    (fn [whole-value]
      (reduce (fn [all-errors path-check]
                (into all-errors (path-check whole-value)))
              []
              compiled-path-checks))))