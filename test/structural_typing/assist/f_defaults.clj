(ns structural-typing.assist.f-defaults
  (:require [structural-typing.assist.defaults :as subject])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))


(fact "the way functions print by default"
  (type! :X {:x even?
             :y (complement even?)})

  (check-for-explanations :X {:x 1 :y 2})
  => (just (err:shouldbe :x "even?" 1)
           (err:shouldbe :y "<custom-predicate>" 2)))

(fact "default error explainer"
  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x]
                                        :leaf-value 3})
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


