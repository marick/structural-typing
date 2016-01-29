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
    (built-like :R [:wrong 4 2 :wrong]) => [:wrong 4 2 :wrong]
    (check-for-explanations :R [:wrong 111 2 :wrong]) => (just (err:shouldbe [1] "even?" 111)))

  (fact "a single range outside of a path"
    (type! :R {(RANGE 1 3) even?})
    (built-like :R [:wrong 4 2 :wrong]) => [:wrong 4 2 :wrong]
    (check-for-explanations :R [:wrong 111 2 :wrong]) => (just (err:shouldbe [1] "even?" 111)))
    
  (fact "a range within an ALL"
    (type! :R {[ALL :x (RANGE 1 3)] even?})
    (check-for-explanations :R [ {:x [:ignored 4 2]} 
                                 {:x [:ignored 1 2 :ignored]}])
    => (just (err:shouldbe [1 :x 1] "even?" 1)))

  (fact "sequences are nil-padded on the right"
    (type! :SECOND-AND-THIRD {[(RANGE 1 3)] [required-path pos?]})
    (built-like :SECOND-AND-THIRD [:ignored 1 2]) => [:ignored 1 2]
    (check-for-explanations :SECOND-AND-THIRD [:ignored 1])
    => (just (err:required [2])))

  (fact "two ranges in a path"
    (type! :X {[:a (RANGE 1 4) :b (RANGE 1 5) pos?] even?})
    (check-for-explanations :X {:a [:wrong :wrong
                                    {:b [1  2  2  2  2 1]}
                                    {:b [1 -1 -1 -1 -1 1]}
                                    :wrong]})
    => (just #"\[:a \(RANGE 1 4\) :b \(RANGE 1 5\) pos\?\] is not a path"))

  (fact "a range can be taken of an infinite sequence"
    (type! :X {(RANGE 1 3) even?})
    (check-for-explanations :X (repeat 1)) => (just (err:shouldbe [1] "even?" 1)
                                                    (err:shouldbe [2] "even?" 1))))


(fact "indexes in paths"
  (fact "describing the whole path"
    (type! :X {[1] even?})
    (built-like :X [1 2 3]) => [1 2 3]
    (check-for-explanations :X [1 3 5]) => (just (err:shouldbe [1] "even?" 3)))

  (fact "as part of a path"
    (type! :X {[:a 2] {:b even?}})
    (built-like :X {:a [0 1 {:b 2}]}) => {:a [0 1 {:b 2}]}
    (check-for-explanations :X {:a [0 1 {:b 1}]}) => (just (err:shouldbe [:a 2 :b] "even?" 1))))


(fact "ONLY"
  (fact "describing the whole path"
    (type! :X {[ONLY] even?})
    (built-like :X [2]) => [2]
    (check-for-explanations :X [1]) => (just (err:shouldbe [ONLY] "even?" 1))
    (check-for-explanations :X []) => (just (err:only []))
    (check-for-explanations :X [1 2]) => (just (err:only [1 2]))
    (check-for-explanations :X 3) => (just (err:notpath [ONLY] 3)))

  (fact "as part of a path"
    (type! :X {[:a ONLY] {:b even?}})
    (built-like :X {:a [{:b 2}]}) => {:a [{:b 2}]}
    (check-for-explanations :X {:a [{:b 1}]}) => (just (err:shouldbe [:a ONLY :b] "even?" 1))
    (check-for-explanations :X {:a [{:b 1}]}) => (just (err:shouldbe [:a ONLY :b] "even?" 1))
    (check-for-explanations :X {:a [3 {:b 1}]}) => (just (err:only [3 {:b 1}]))
    (check-for-explanations :X {:a []}) => (just (err:only []))
    (check-for-explanations :X {:a 3}) => (just (err:notpath [:a ONLY :b] {:a 3}))))


(start-over!)
