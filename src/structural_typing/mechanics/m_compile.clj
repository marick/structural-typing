(ns structural-typing.mechanics.m-compile
  (:require [blancas.morph.monads :as e]
            [structural-typing.api.predicates :as pred]
            [com.rpl.specter :as specter]
            [structural-typing.mechanics.m-lifting-predicates :refer [lift]]))

(defn compile-predicates [preds]
  (let [lifted (map lift preds)
        combined (apply juxt lifted)]
    (comp e/lefts combined)))

;; TODO: This code could be made tenser. It cries out for transients.

(defn results-for-one-path [whole-value leaf-values original-path errors-fn]
  (reduce (fn [so-far leaf-value]
            (into so-far (->> leaf-value
                             (assoc {:whole-value whole-value :path original-path} :leaf-value)
                             errors-fn)))
          []
          leaf-values))


(defn compile-type [t]
  (let [processed-triples (map (fn [[path preds]]
                                 (vector path
                                         (apply specter/comp-paths path)
                                         (compile-predicates preds)))
                               t)]

    (fn [object-to-check]
      (reduce (fn [all-errors [original-path compiled-path errors-fn]]
                (into all-errors (results-for-one-path object-to-check
                                                      (specter/compiled-select compiled-path object-to-check)
                                                      original-path
                                                      errors-fn)))
              []
              processed-triples))))
