(ns structural-typing.guts.ftd-right-hand-side
  "Tests that show different variants of the right-hand side"
  (:require [structural-typing.guts.type-descriptions :as subject])
  (:require [midje.sweet :refer :all]))

(future-fact "simple case - just a vector of predicates"
  (subject/canonicalize [{:a [odd? even?]}] {}) => {[:a] [odd? even?]})
  
