(ns structural-typing.evaluate
  (:require 
            [blancas.morph.monads :as e]
            [structural-typing.api.predicates :as pred]))

(defn friendly-path [path]
  (if (= 1 (count path)) (first path) path))




;; (defn lift-predicates [raw-preds]
;;   (let [preds (map lift raw-preds)]
;;     (fn [leaf-value-context]
;;       (map preds (repeat leaf-value-context))))
;;       (loop [preds preds]
;;         (if (empty? preds)
;;           []
;;           (let [
;;                 (cond (empty? preds)
;;                       []

            
            
          
;;     []))


;; (defn one-type [structure type-map]
;;   (map #(one-path structure %) type-map))
