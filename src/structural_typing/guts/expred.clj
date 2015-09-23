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

(defn default-predicate-explainer
  "Converts an [[oopsie]] into a string of the form \"%s should be %s; it is %s\"."
  [{:keys [predicate-string leaf-value] :as expred}]
  (format "%s should be `%s`; it is `%s`"
          (friendly-path expred)
          predicate-string
          (pr-str leaf-value)))

