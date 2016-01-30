(ns structural-typing.use.condensed-type-descriptions.f-maps
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)

(fact "nested maps are flattened"
  (type! :X {:a {:b even?}})
  (type! :Y {[:a :b] even?})
  (description :X) => (description :Y))

(fact "both the path and a right-hand-side map can have paths"
  (type! :X {[:a :b] {[:c :d] even?}})
  (type! :Y {[:a :b :c :d] even?})
  (description :X) => (description :Y))

(fact "a path implied by a map can match an explicit path"
  (type! :X {[:a :b] pos?
             :a {:b even?}})
  (description :X) => (just {[:a :b] (just '[pos? even?] :in-any-order)}))

(fact "ALL can be inserted into a path by using it as a map key"
  (type! :X {:points {ALL {:x integer? :y integer?}}})
  (type! :Y {[:points ALL :x] integer?
             [:points ALL :y] integer?})
  (description :X) => (description :Y))
  
(fact "because maps have semantics, you have to wrap them to use them as values to compare"
  (type! :Unwrapped {:unwrapped {:x 1}})
  (type! :Wrapped {:wrapped (pred/exactly {:x 1})})

  (fact "The success case is no different"
    (built-like :Unwrapped {:unwrapped {:x 1}}) => {:unwrapped {:x 1}}
    (built-like :Wrapped {:wrapped {:x 1}}) => {:wrapped {:x 1}})

  (fact "but the failure case produces a different error message"
    (check-for-explanations :Unwrapped {:unwrapped {:x 2}})
    => (just (err:shouldbe [:unwrapped :x] "exactly `1`" 2 :omit-quotes))
    (check-for-explanations :Wrapped {:wrapped {:x 2}})
    => (just (err:shouldbe :wrapped "exactly `{:x 1}`" "{:x 2}" :omit-quotes))))

(start-over!)
