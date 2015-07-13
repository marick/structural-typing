(ns ^:no-doc structural-typing.mechanics.compiling-types
  (:require [blancas.morph.monads :as e]
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
                                 :leaf-value leaf-value}]
              (->> value-context
                   run-path-preds
                   (into so-far))))
          []
          (map vector leaf-values (range))))

(defn oopsies-for-bad-path [whole-value original-path]
  (let [base-oopsie {:whole-value whole-value
                     :path original-path
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

(defn compile-path [path]
  [(apply specter/comp-paths path) identity (fn [selected-value]
                                              path)])

(defn compile-path-check [[original-path preds]]
  (let [compiled-preds (compile-predicates preds)
        [compiled-path leaf-value-selector specific-path-maker] (compile-path original-path)]
    (fn [object-to-check]
      (e/either [x (run-select compiled-path object-to-check)]
                (oopsies-for-bad-path object-to-check original-path)
                (oopsies-for-one-path object-to-check
                                      (leaf-value-selector x)
                                      original-path
                                      compiled-preds)))))

(defn compile-type [t]
  ;; Note that the path-checks are compiled once, returning a function to be run often.
  (let [compiled-path-checks (map compile-path-check t)]
    (fn [object-to-check]
      (reduce (fn [all-errors path-check]
                (into all-errors (path-check object-to-check)))
              []
              compiled-path-checks))))
