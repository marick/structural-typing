(ns structural-typing.api.f-custom
  (:require [structural-typing.api.custom :as subject]
            [structural-typing.api.path :as path])
  (:use midje.sweet))

(fact "friendly-function-name"
  (subject/friendly-function-name even?) => "even?"
  (subject/friendly-function-name (fn [])) => "your custom predicate"
  (subject/friendly-function-name :key) => ":key"
  (subject/friendly-function-name #'even?) => "even?"

  (let [f ( ( (fn [a] (fn [b] (fn [c] (+ a b c)))) 1) 2)]
    (subject/friendly-function-name f) => "your custom predicate")

  (let [f ( ( (fn [a] (fn [b] (fn my:tweedle-dum [c] (+ a b c)))) 1) 2)]
    (subject/friendly-function-name f) => "my:tweedle-dum"))




(fact "friendly paths"
  (subject/friendly-path {:path [:a]}) => ":a"
  (subject/friendly-path {:path [1]}) => "[1]"
  (subject/friendly-path {:path [:a :b]}) => "[:a :b]"
  (subject/friendly-path {:path [:a path/ALL :b]}) => "[:a ALL :b]"
  (subject/friendly-path {:path [:a even? :b]}) => "[:a even? :b]"
  (subject/friendly-path {:path [:a 0 :b]}) => "[:a 0 :b]")

(future-fact "handle other specter path components, including plain functions and vars")

