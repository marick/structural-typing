(ns ^:no-doc structural-typing.mechanics.compiling-types
  (:require [blancas.morph.monads :as e]
            [structural-typing.api.predicates :as pred]
            [com.rpl.specter :as specter]
            [structural-typing.api.custom :as custom]
            [structural-typing.mechanics.lifting-predicates :refer [lift]]))

(defn compile-predicates [preds]
  (let [lifted (map lift preds)
        combined (apply juxt lifted)]
    (comp e/lefts combined)))

;; TODO: This code could be made tenser. It cries out for transients.

(defn oopsies-for-one-path [whole-value leaf-values original-path run-path-preds]
  (reduce (fn [so-far [leaf-value index]]
            (let [value-context {:whole-value whole-value
                                 :path original-path
                                 :leaf-index index
                                 :leaf-count (count leaf-values)
                                 :leaf-value leaf-value}]
              (->> value-context
                   run-path-preds
                   (into so-far))))
          []
          (map vector leaf-values (range))))

(defn oopsies-for-bad-path [whole-value original-path]
  (let [base-oopsie {:whole-value whole-value
                     :path original-path
                     :leaf-index 0
                     :leaf-count 1
                     :leaf-value nil}]
    (-> base-oopsie
    (assoc :predicate-explainer (constantly 
                                 (format "%s is not a path into `%s`"
                                         ;; ick.
                                         (custom/friendly-path base-oopsie)
                                         (pr-str whole-value))))
    vector)))

(defn run-select [compiled-path object-to-check]
  (e/make-either (specter/compiled-select compiled-path object-to-check)))

(defn compile-type [t]
  (let [processed-triples (map (fn [[path preds]]
                                 (vector path
                                         (apply specter/comp-paths path)
                                         (compile-predicates preds)))
                               t)]

    (fn [object-to-check]
      (reduce (fn [all-errors [original-path compiled-path run-path-preds]]
                (into all-errors
                      (e/either [x (run-select compiled-path object-to-check)]
                                (oopsies-for-bad-path object-to-check original-path)
                                (oopsies-for-one-path object-to-check
                                                      x
                                                      original-path
                                                      run-path-preds))))
              []
              processed-triples))))
