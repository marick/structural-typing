(ns structural-typing.api.f-defaults
  (:require [structural-typing.api.defaults :as subject]
            [structural-typing.api.path :as path])
  (:use midje.sweet))

(defn path-n-of [path leaf-index leaf-count]
  {:path path :leaf-index leaf-index :leaf-count leaf-count})
(defn simple-path [path] (path-n-of path 0 1))


(fact "friendly-predicate-name"
  (subject/friendly-function-name even?) => "even?"
  (subject/friendly-function-name (fn [])) => "your custom predicate"
  (subject/friendly-function-name :key) => ":key"
  (subject/friendly-function-name #'even?) => "even?"

  (let [f ( ( (fn [a] (fn [b] (fn [c] (+ a b c)))) 1) 2)]
    (subject/friendly-function-name f) => "your custom predicate")

  (let [f ( ( (fn [a] (fn [b] (fn my:tweedle-dum [c] (+ a b c)))) 1) 2)]
    (subject/friendly-function-name f) => "my:tweedle-dum"))




(future-fact "default error explainer"
  (subject/default-predicate-explainer {:predicate-string "core/even?"
                                               :path [:x]
                                               :leaf-value 3})
  => ":x should be `core/even?`; it is `3`"


  (subject/default-predicate-explainer {:predicate-string "core/even?"
                                               :path [:x ALL :y]
                                               :leaf-value 3
                                               :index-string "[0]"})
  => "[:x ALL :y] should be `core/even?`; it is `3`"
)

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

