(ns ^:no-doc structural-typing.guts.compile.compile
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.compile.to-specter-path :as to-specter-path]
            [com.rpl.specter :as specter]
            [structural-typing.guts.self-check :as self :refer [returns-many]]
            [structural-typing.guts.preds.wrap :as wrap]))

(defn compile-predicates [preds]
  (let [lifted (map wrap/lift preds)]
    (fn [value-holder]
      (->> (reduce #(into %1 (%2 value-holder)) [] lifted)
           (returns-many :oopsie)))))


(defn compile-type [type-map]
  ;; Note that the path-checks are compiled once, returning a function to be run often.
  (let [one-check (fn [[path preds]]
                    (to-specter-path/mkfn:whole-value->oopsies path
                                                               (compile-predicates preds)))
        compiled-path-checks (map one-check type-map)]
    (fn [whole-value]
      (reduce (fn [all-errors path-check]
                (into all-errors (path-check whole-value)))
              []
              compiled-path-checks))))
