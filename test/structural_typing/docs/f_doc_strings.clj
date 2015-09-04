(ns structural-typing.docs.f-doc-strings
  (:require [structural-typing.type :refer :all]
            [structural-typing.global-type :refer :all]
            [structural-typing.preds :as pred])
  (:use midje.sweet structural-typing.assist.testutil))

(start-over!)

(type! :Point {:x integer? :y integer?})
(type! :X (pred/implies :a {:b [required-key (includes :Point)]}))

(fact "implies works with included types"
  (checked :X {:a 1 :b {:x 1, :y 1}}) => {:a 1 :b {:x 1, :y 1}}
  (check-for-explanations :X {:a 1}) => (just [":b must exist and be non-nil"])
  (fact "note that :X does *not* require keys of :Point to be present"
    (checked :X {:a 1 :b {:x 1}}) => {:a 1 :b {:x 1}}))

  
