(ns structural-typing.api.f-predicates
  (:require [structural-typing.api.predicates :as subject])
  (:use midje.sweet))

;; (fact "friendly-name"
;;   (subject/friendly-name even?) => "core/even?"
;;   (subject/friendly-name (fn [])) => "your custom predicate"
;;   (subject/friendly-name :key) => :key
;;   (subject/friendly-name #'even?) => "even?"

;;   (let [f ( ( (fn [a] (fn [b] (fn [c] (+ a b c)))) 1) 2)]
;;     (subject/friendly-name f) => "your custom predicate")

;;   (let [f ( ( (fn [a] (fn [b] (fn my:tweedle-dum [c] (+ a b c)))) 1) 2)]
;;     (subject/friendly-name f) => "my:tweedle-dum"))

