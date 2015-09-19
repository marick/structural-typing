(ns structural-typing.use.condensed-type-descriptions.f-ALL
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)

(fact "error messages show indexes"
  (type! :A-has-evens {[:a ALL] even?})
  (check-for-explanations :A-has-evens {:a [1 2]}) => [(err:shouldbe [:a 0] "even?" 1)]

  (type! :DoubleNested {[:a ALL :b ALL] even?})  
  (check-for-explanations :DoubleNested {:a [{:b [4 8]} {:b [0 2]} {:b [1 2 4]}]})
  => [(err:shouldbe [:a 2 :b 0] "even?" 1)])

(start-over!)
