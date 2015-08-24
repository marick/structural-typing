(ns ^:no-doc structural-typing.guts.mechanics.compiling-types
  (:refer-clojure :exclude [compile])
  (:require [com.rpl.specter :as specter]
            [structural-typing.pred-writing.shapes.self-check :as self :refer [returns-many]]
            [structural-typing.pred-writing.shapes.oopsie :as oopsie]
            [structural-typing.guts.paths.substituting :as path]
            [structural-typing.guts.shapes.pred :as pred]))

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
           :path (path/replace-with-indices path path-adjustment)
           :specter-path path))
           
)

(defn compile-predicates [preds]
  (let [lifted (map pred/lift preds)]
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
  (let [match-many? (path/path-will-match-many? original-path)
        path-adjustment (if match-many? path/force-collection-of-indices identity)
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
