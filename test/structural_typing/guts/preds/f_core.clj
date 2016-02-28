(ns structural-typing.guts.preds.f-core
  (:require [structural-typing.guts.preds.core :as subject]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.guts.preds.wrap :as wrap])
  (:require [such.readable :as readable])
  (:use midje.sweet structural-typing.assist.testutil))

(facts "required-path starts out lifted"
  (subject/required-path (exval 5)) => []
  (readable/fn-string subject/required-path) => "required-path"
  (let [result (subject/required-path (exval nil [:x]))]
    result => (just (oopsie-for nil :predicate-string "required-path"))
    (oopsie/explanations result) => (just ":x must exist and be non-nil")))



(facts "not-nil forces a value to be non-nil"
  (subject/not-nil (exval 5)) => []
  (readable/fn-string subject/not-nil) => "not-nil"
  
  (let [result (subject/not-nil (exval nil [:x]))]
    result => (just (oopsie-for nil :predicate-string "not-nil"))
    (oopsie/explanations result) => (just ":x is nil, and that makes Sir Tony Hoare sad"))

  (let [result (subject/not-nil (exval nil []))]
    result => (just (oopsie-for nil :predicate-string "not-nil"))
    (oopsie/explanations result) => (just "Value is nil, and that makes Sir Tony Hoare sad")))


(fact "predicates can be classified as special-case handlers"
  (let [neither even?
        both (subject/rejects-missing-and-nil even?)
        reject-missing (subject/rejects-missing even?)
        reject-nil (subject/rejects-nil even?)]
    (subject/special-case-handling neither) => {}
    (subject/special-case-handling both) => {:reject-missing? true :reject-nil? true}
    (subject/special-case-handling reject-missing) => {:reject-missing? true :reject-nil? false}
    (subject/special-case-handling reject-nil) => {:reject-missing? false :reject-nil? true}))
