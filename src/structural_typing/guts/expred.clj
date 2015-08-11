(ns ^:no-doc structural-typing.guts.expred
    (:require [such.readable :as readable]
              [structural-typing.guts.frob :as frob]
              [structural-typing.guts.preds.annotated :as annotated]
              [structural-typing.surface.defaults :as defaults]))


(defrecord ExPred [predicate predicate-string explainer])

(defn from-pred [pred]
  (hash-map :explainer (annotated/get-explainer pred)
            :predicate-string (annotated/get-predicate-string pred)
            :predicate (annotated/get-predicate pred)))


