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

(defn oopsies-for-one-path [whole-value leaf-values original-path per-path-error-maps]
  (reduce (fn [so-far [leaf-value index]]
            (let [value-context {:whole-value whole-value
                                 :path original-path
                                 :leaf-index index
                                 :leaf-count (count leaf-values)
                                 :leaf-value leaf-value}]
              (->> value-context
                   per-path-error-maps
                   (into so-far))))
          []
          (map vector leaf-values (range))))


(defn compile-type [t]
  (let [processed-triples (map (fn [[path preds]]
                                 (vector path
                                         (apply specter/comp-paths path)
                                         (compile-predicates preds)))
                               t)]

    (fn [object-to-check]
      (reduce (fn [all-errors [original-path compiled-path per-path-error-maps]]
                (let [selected-values (specter/compiled-select compiled-path object-to-check)
                      oopsies (oopsies-for-one-path object-to-check
                                                    selected-values
                                                    original-path
                                                    per-path-error-maps)]
                  (into all-errors oopsies)))
              []
              processed-triples))))
