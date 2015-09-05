(ns structural-typing.use.f-defaults
  (:require [structural-typing.defaults :as subject]
            [such.readable :as readable])
  (:use midje.sweet))


(fact "the way functions print"
  (readable/fn-string even?) => "even?"
  (readable/fn-string (fn [])) => "<custom-predicate>"
  (readable/fn-string :key) => ":key"
  (readable/fn-string #'even?) => "even?"

  (let [f ( ( (fn [a] (fn [b] (fn [c] (+ a b c)))) 1) 2)]
    (readable/fn-string f) => "<custom-predicate>")

  (let [f ( ( (fn [a] (fn [b] (fn my:tweedle-dum [c] (+ a b c)))) 1) 2)]
    (readable/fn-string f) => "my:tweedle-dum"))

