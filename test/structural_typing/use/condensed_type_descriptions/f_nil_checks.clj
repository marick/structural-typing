(ns structural-typing.use.condensed-type-descriptions.f-nil-checks
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil)
  (:refer-clojure :except [any?]))


(start-over!)

(fact "It is possible to check for nil values (even though those are normally optional)"
  (type! :X {:x nil?})
  (built-like :X {:x nil}) => {:x nil}
  (built-like :X {}) => {}
  (check-for-explanations :X {:x 1}) => (just (err:shouldbe :x "nil?" 1)))


(start-over!)
