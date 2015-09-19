(ns structural-typing.assist.f-defaults
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
