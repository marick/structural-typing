(ns ^:no-doc structural-typing.guts.compile.compile
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.compile.compile-path :as path]
            [structural-typing.guts.self-check :as self :refer [returns-many]]
            [structural-typing.guts.preds.pseudopreds :as pseudo]
            [structural-typing.guts.exval :as exval]
            [structural-typing.guts.preds.wrap :as wrap]))

(defn compile-predicates [preds]
  (let [lifted (map wrap/lift preds)]
    (fn [value-holder]
      (->> (reduce #(into %1 (%2 value-holder)) [] lifted)
           (returns-many :oopsie)))))

(defn compile-pair [[path preds]]
  (vector (path/compile-path path (pseudo/max-rejection preds))
          (compile-predicates (pseudo/without-pseudopreds preds))))


(defn produce-oopsies [compiled-preds possible-exvals]
  (reduce (fn [so-far possible-exval]
            (if (wrap/oopsie? possible-exval)
              (conj so-far possible-exval)
              (into so-far (compiled-preds possible-exval))))
          []
          possible-exvals))

(defn run-compiled-pairs [whole-value pairs]
  (reduce (fn [so-far [compiled-path compiled-preds]]
            (let [exvals (path/apply-path compiled-path whole-value)]
              (into so-far (produce-oopsies compiled-preds exvals))))
          []
          pairs))

(defn compile-type [type-map]
  (let [compiled-pairs (map compile-pair type-map)]
    (fn [whole-value]
      (run-compiled-pairs whole-value compiled-pairs))))
