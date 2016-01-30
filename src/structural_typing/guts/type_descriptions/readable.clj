(ns ^:no-doc structural-typing.guts.type-descriptions.readable
  (:use structural-typing.clojure.core)
  (:require [such.readable :as readable]))


(defn- stringify [components]
  (if (and (= 1 (count components))
           (keyword? (first components)))
    (str (first components))
    (cl-format nil "[~{~S~^ ~}]" components)))

(defn friendly [path]
  (->> path
       readable/value
       stringify))

