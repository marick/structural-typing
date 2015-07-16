(ns ^:no-doc structural-typing.guts.mechanics.compiling-types
  (:require [blancas.morph.monads :as e]
            [com.rpl.specter :as specter]
            [structural-typing.surface.oopsie :as oopsie]
            [structural-typing.guts.paths.substituting :as path]
            [structural-typing.guts.mechanics.lifting-predicates :refer [lift]]))

(defn compile-predicates [preds]
  (let [lifted (map lift preds)
        combined (apply juxt lifted)]
    (comp e/lefts combined)))

;; TODO: This code could be made tenser. It cries out for transients.

(defn oopsies-for-bad-path [whole-value original-path]
  (let [base-oopsie {:whole-value whole-value
                     :original-path original-path
                     :path original-path
                     :leaf-value nil}]
    (-> base-oopsie
    (assoc :predicate-explainer (constantly 
                                 (format "%s is not a path into `%s`"
                                         ;; ick.
                                         (oopsie/friendly-path base-oopsie)
                                         (pr-str whole-value))))
    vector)))

(defn run-select [compiled-path object-to-check]
  (e/make-either (specter/compiled-select compiled-path object-to-check)))

(defn compile-path [path]
  (if (path/path-will-match-many? path)
      (vector (apply specter/comp-paths (path/force-collection-of-indices path))
              last
              (let [replacement-points (path/replacement-points path)]
                #(path/replace-with-indices path
                                            replacement-points
                                            (butlast %))))
      (vector (apply specter/comp-paths path) identity (constantly path))))

;; Previous and following functions are factored wrong.

(defn compile-path-check [[original-path preds]]
  (let [compiled-preds (compile-predicates preds)
        [compiled-path leaf-value-selector specific-path-maker] (compile-path original-path)]
    (letfn [(oopsies-for-leaf [whole-value leaf]
              (let [value-context {:whole-value whole-value
                                   :original-path original-path
                                   :leaf-value (leaf-value-selector leaf)}
                    oopsies (compiled-preds value-context)]
                (map #(assoc % :path (specific-path-maker leaf)) oopsies)))]
      (fn [object-to-check]
        (e/either [leafs-to-check (run-select compiled-path object-to-check)]
                     (oopsies-for-bad-path object-to-check original-path)
                     (mapcat #(oopsies-for-leaf object-to-check %1)
                             leafs-to-check))))))

(defn compile-type [t]
  ;; Note that the path-checks are compiled once, returning a function to be run often.
  (let [compiled-path-checks (map compile-path-check t)]
    (fn [object-to-check]
      (reduce (fn [all-errors path-check]
                (into all-errors (path-check object-to-check)))
              []
              compiled-path-checks))))
