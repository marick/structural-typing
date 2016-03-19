(ns structural-typing.assist.predicate-defining
  "Help for defining custom predicates"
  (:use structural-typing.clojure.core)
  (:require [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.assist.format :as format]
            [structural-typing.guts.preds.annotating :as annotating]
            [such.readable :as readable]))

(defn should-be [format-string expected]
  #(format format-string,
           (oopsie/friendly-path %)
           (readable/value-string expected)
           (readable/value-string (:leaf-value %))))

(defn compose-predicate [name pred fmt-fn]
  (->> pred
       (annotating/show-as name)
       (annotating/explain-with fmt-fn)))
