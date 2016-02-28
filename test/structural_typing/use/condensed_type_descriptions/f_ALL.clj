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

(fact "A solitary ALL"
  (type! :IntArray {[ALL] integer?})
  (built-like? :IntArray [1 2 3 4]) => true
  (check-for-explanations :IntArray [1 :a 2 :b])
  => (just (err:shouldbe [1] "integer?" :a)
           (err:shouldbe [3] "integer?" :b)))


(fact "ALL following ALL"
  (type! :D2 {[ALL ALL] integer?})
  (check-for-explanations :D2 [  [0 :elt-0-1] [:elt-1-0] [] [0 0 :elt-3-2]])
  => (just (err:shouldbe [0 1] "integer?" :elt-0-1)
           (err:shouldbe [1 0] "integer?" :elt-1-0)
           (err:shouldbe [3 2] "integer?" :elt-3-2))

  (type! :Nesty {[:x ALL ALL :y] integer?})
  (check-for-explanations :Nesty {:x [ [{:y 1}] [{:y :notint}]]})
  => [(err:shouldbe [:x 1 0 :y] "integer?" :notint)]

  (check-for-explanations :Nesty {:x [1]})
  =future=> (just #"\[:x SOME ALL :y\] is not a path"))

(fact "ALL requires collections that are not maps"
  :current
  (built-like {[ALL] pos?} #{}) => #{}
  (built-like {[ALL] pos?} []) => []
  (built-like {[ALL] pos?} (map inc [1 2])) => [2 3]
  (check-for-explanations {[ALL] pos?} 1) => (just (err:bad-all-target [ALL] 1 1))
  (check-for-explanations {[ALL] pos?} {:a 1, :b 2})
  => (just (err:bad-all-target [ALL] {:a 1, :b 2} {:a 1, :b 2})))

(start-over!)
