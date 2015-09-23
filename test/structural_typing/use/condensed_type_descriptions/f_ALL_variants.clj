(ns structural-typing.use.condensed-type-descriptions.f-ALL-variants
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)

;; Most of the checking is common to these and ALL.

(fact "RANGE"
  (fact "a single range"
    (type! :R {[(RANGE 1 3)] even?})
    (checked :R [:wrong 4 2 :wrong]) => [:wrong 4 2 :wrong]
    (check-for-explanations :R [:wrong 111 2 :wrong]) => (just (err:shouldbe [1] "even?" 111)))

  (fact "a single range outside of a path"
    (type! :R {(RANGE 1 3) even?})
    (checked :R [:wrong 4 2 :wrong]) => [:wrong 4 2 :wrong]
    (check-for-explanations :R [:wrong 111 2 :wrong]) => (just (err:shouldbe [1] "even?" 111)))
    
  (fact "a range within an ALL"
    (type! :R {[ALL :x (RANGE 1 3)] even?})
    (check-for-explanations :R [ {:x [:ignored 4 2]} 
                                 {:x [:ignored 1 2 :ignored]}])
    => (just (err:shouldbe [1 :x 1] "even?" 1)))

  (fact "it is a type failure if the range extends beyond the count of elements"
    (type! :SECOND-AND-THIRD {[(RANGE 1 3)] pos?})
    (checked :SECOND-AND-THIRD [:ignored 1 2]) => [:ignored 1 2]
    (check-for-explanations :SECOND-AND-THIRD [:ignored 1])
    => (just #"\[\(RANGE 1 3\)\] is not a path into `\[:ignored 1\]`"))

  (fact "two ranges in a path"
    (type! :X {[:a (RANGE 1 4) :b (RANGE 1 5) pos?] even?})
    (check-for-explanations :X {:a [:wrong :wrong
                                    {:b [1  2  2  2  2 1]}
                                    {:b [1 -1 -1 -1 -1 1]}
                                    :wrong]})
    => (just #"\[:a \(RANGE 1 4\) :b \(RANGE 1 5\) pos\?\] is not a path")))


(fact "indexes in paths"
  (fact "describing the whole path"
    (type! :X {[1] even?})
    (checked :X [1 2 3]) => [1 2 3]
    (check-for-explanations :X [1 3 5]) => (just (err:shouldbe [1] "even?" 3)))

  (fact "as part of a path"
    (type! :X {[:a 2] {:b even?}})
    (checked :X {:a [0 1 {:b 2}]}) => {:a [0 1 {:b 2}]}
    (check-for-explanations :X {:a [0 1 {:b 1}]}) => (just (err:shouldbe [:a 2 :b] "even?" 1))
    )

  (fact "the index does not exist in the value"
    (type! :X {[:a 2] {:b even?}})
    (check-for-explanations :X {:a [0 {:b 1}]}) => (just #"\[:a 2 :b\] is not a path into")))

(start-over!)
