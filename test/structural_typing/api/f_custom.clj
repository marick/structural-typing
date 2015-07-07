(ns structural-typing.api.f-custom
  (:require [structural-typing.api.custom :as subject]
            [structural-typing.api.path :as path])
  (:use midje.sweet))

(defn path-n-of [path leaf-index leaf-count]
  {:path path :leaf-index leaf-index :leaf-count leaf-count})
(defn simple-path [path] (path-n-of path 0 1))


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
  (subject/friendly-path (simple-path [:a])) => ":a"
  (subject/friendly-path (simple-path [:a :b])) => "[:a :b]"
  (subject/friendly-path (simple-path [:a path/ALL :b])) => "[:a ALL :b]")

(fact "cases with indices"
  (subject/friendly-path (path-n-of [:a] 0 5)) => ":a[0]"
  (subject/friendly-path (path-n-of [:a] 1 5)) => ":a[1]"

  (subject/friendly-path (path-n-of [:a :b] 0 5)) => "[:a :b][0]"
  (subject/friendly-path (path-n-of [:a :b] 4 5)) => "[:a :b][4]"

  (subject/friendly-path (path-n-of [:a path/ALL :b] 0 5)) => "[:a ALL :b][0]"
  (subject/friendly-path (path-n-of [:a path/ALL :b] 3 20)) => "[:a ALL :b][3]")

(future-fact "handle other specter path components, including plain functions and vars")

