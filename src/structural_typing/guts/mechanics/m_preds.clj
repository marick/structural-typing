(ns ^:no-doc structural-typing.guts.mechanics.m-preds
  (:require [structural-typing.guts.mechanics.lifting-predicates :as lift]
            [structural-typing.guts.frob :as frob]
            [structural-typing.guts.preds.annotated :as annotated]
            [structural-typing.surface.oopsie :as oopsie]))


(def show-as annotated/show-as)
(def explain-with annotated/explain-with)

(defn compose-predicate [name pred fmt-fn]
  (->> pred
       (annotated/show-as name)
       (explain-with fmt-fn)))

(def required-key
  "False iff a key/path does not exist or has value `nil`. This is the only
   predicate that is not considered optional."

  (-> (compose-predicate "required-key"
                         (comp not nil?)
                         #(format "%s must exist and be non-nil" (oopsie/friendly-path %)))
      (lift/lift* false)))


