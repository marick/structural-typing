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

(fact "When traversing paths reveals that location of ALL is not a collection"
  (type! :Points {[ALL :x] integer?
                  [ALL :y] integer?})
  (check-for-explanations :Points 3) => (just #"\[ALL :x] is not a path into `3`"
                                              #"\[ALL :y] is not a path into `3`")

  (future-fact "Failure is annoying side effect of there being no distinction between a present nil and a missing key"

    (check-for-explanations :Points [1 2 3]) => (just #"\[ALL :x] is not a path into `3`"
                                                      #"\[ALL :y] is not a path into `3`"))

  (fact "works for partial collections"
    (type! :Figure (requires :color [:points ALL (each-of :x :y)]))
    (check-for-explanations :Figure {:points 3})
    => (just (err:required :color)
             #"\[:points ALL :x\] is not a path into `\{:points 3\}`"
             #"\[:points ALL :y\] is not a path into `\{:points 3\}`")))

(start-over!)
