(ns structural-typing.assist.f-defaults
  (:require [structural-typing.assist.defaults :as subject])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil)
  (:refer-clojure :except [any?]))

(fact "special cases for printing"
  (type! :X {:x even?
             :y (complement even?)})

  (fact "the checking predicates are printed nicely"
    (check-for-explanations :X {:x 1 :y 2})
    => (just ":x should be `even?`; it is `1`"
             ":y should be `<custom-predicate>`; it is `2`"))

  (fact "a defined predicate as the incorrect value also prints nicely"
    (check-for-explanations :X {:x even?})
    => (just ":x should be `even?`; it is the function `even?`"))

  (fact "a predicate as the incorrect value also prints nicely"
    (let [built-fn (complement even?)
          msgs (check-for-explanations :X {:y built-fn})]
      msgs => (just #"it is the function `")
      (.contains (first msgs) (pr-str built-fn)))))


  
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


