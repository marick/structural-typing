(ns structural-typing.use.condensed-type-descriptions.f-maps
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil)
  (:refer-clojure :except [any?]))


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
  
(start-over!)
