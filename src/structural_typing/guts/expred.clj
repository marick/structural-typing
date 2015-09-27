(ns ^:no-doc structural-typing.guts.expred
    (:require [structural-typing.guts.type-descriptions.readable :as readable-path]))


(def required-keys #{:predicate :predicate-string :explainer})
(defrecord ExPred [predicate predicate-string explainer])

(defn friendly-path
  "Convert the oopsie's path into a string, with predicates and function components printed nicely."
  [oopsie]
  (let [path (:path oopsie)]
    (if (empty? path)
      "Value"
      (readable-path/friendly path))))

