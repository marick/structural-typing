(ns structural-typing.use.condensed-type-descriptions.f-ALL-variants
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

;; Most of the checking is common to these and ALL.

(fact "RANGE"
  (type! :R {[(RANGE 1 3)] even?})
  (checked :R [:wrong 4 2 :wrong]) => [:wrong 4 2 :wrong]

  (type! :R {[ALL :x (RANGE 1 3)] even?})
  (check-for-explanations :R [ {:x [:ignored 4 2]} 
                               {:x [:ignored 1 2 :ignored]}])
  => (just (err:shouldbe [1 :x 1] "even?" 1))

  (type! :SECOND-AND-THIRD {[(RANGE 1 3)] pos?})
  (checked :SECOND-AND-THIRD [:ignored 1 2]) => [:ignored 1 2]
  (check-for-explanations :SECOND-AND-THIRD [:ignored 1])
  => (just #"\[\(RANGE 1 3\)\] is not a path into `\[:ignored 1\]`"))

