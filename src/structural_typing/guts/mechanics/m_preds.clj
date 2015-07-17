(ns ^:no-doc structural-typing.guts.mechanics.m-preds
  (:require [structural-typing.guts.preds.lifted :as lift]
            [structural-typing.guts.frob :as frob]
            [structural-typing.guts.preds.annotated :as annotated]
            [structural-typing.surface.oopsie :as oopsie]))


(defn compose-predicate [name pred fmt-fn]
  (->> pred
       (annotated/show-as name)
       (annotated/explain-with fmt-fn)))


