(ns structural-typing.pred-writing.f-core-preds
  (:require [structural-typing.pred-writing.core-preds :as subject]
            [structural-typing.pred-writing.shapes.oopsie :as oopsie])
  (:require [such.readable :as readable])
  (:use midje.sweet structural-typing.pred-writing.testutil))

(facts "required-key starts out lifted"
  (subject/required-key (exval 5)) => []
  (readable/fn-string subject/required-key) => "required-key"
  (let [result (subject/required-key (exval nil [:x]))]
    result => (just (oopsie-for nil :predicate-string "required-key"))
    (oopsie/explanations result) => (just ":x must exist and be non-nil")))


