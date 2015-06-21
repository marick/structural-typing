(ns structural-typing.api.predicates
  (:require [structural-typing.mechanics.m-lifting-predicates :as lift]))

;; Utilities

(defn show-as [name f]
  (-> f
      lift/stash-defaults
      (lift/replace-predicate-string name)))

(defn explain-with [explainer f]
  (-> f
      lift/stash-defaults
      (lift/replace-explainer explainer)))


;;; Useful predefined predicates

(defn must-exist [val]
  (not (nil? val)))
