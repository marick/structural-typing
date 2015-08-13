(ns structural-typing.pred-writing.f-defaults
  (:require [structural-typing.pred-writing.defaults :as subject]
            [such.readable :as readable]
            [structural-typing.guts.paths.elements :refer [ALL]])
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


(fact "default error explainer"
  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x]
                                        :leaf-value 3
                                        })
  => ":x should be `even?`; it is `3`"

  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x ALL :y]
                                        :leaf-value 3})
  => "[:x ALL :y] should be `even?`; it is `3`"


  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x 0 :y]
                                        :leaf-value 3})
  => "[:x 0 :y] should be `even?`; it is `3`"

  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x odd? :y]
                                        :leaf-value 3})
  => "[:x odd? :y] should be `even?`; it is `3`")


