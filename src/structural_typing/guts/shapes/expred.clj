(ns ^:no-doc structural-typing.guts.shapes.expred
    (:require [such.readable :as readable]
              [structural-typing.guts.frob :as frob]
              [structural-typing.guts.shapes.pred :as pred]))

(defrecord ExPred [predicate predicate-string explainer])

(defn from-pred [pred]
  (hash-map :explainer (pred/get-explainer pred)
            :predicate-string (pred/get-predicate-string pred)
            :predicate (pred/get-predicate pred)))


