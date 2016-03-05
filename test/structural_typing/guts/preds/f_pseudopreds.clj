(ns structural-typing.guts.preds.f-pseudopreds
  (:require [structural-typing.guts.preds.pseudopreds :as subject]
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



(fact "predicates can be classified as special-case handlers"
  (let [neither even?
        both (subject/rejects-missing-and-nil even?)
        reject-missing (subject/rejects-missing even?)
        reject-nil (subject/rejects-nil even?)]
    (subject/special-case-handling neither) => {}
    (subject/special-case-handling both) => {:reject-missing? true :reject-nil? true}
    (subject/special-case-handling reject-missing) => {:reject-missing? true :reject-nil? false}
    (subject/special-case-handling reject-nil) => {:reject-missing? false :reject-nil? true}))



(fact "find the maximum 'rejectionism' of a list of preds"
  (subject/max-rejection []) => {:reject-nil? false :reject-missing? false}

  (subject/max-rejection [subject/required-path]) => {:reject-nil? true :reject-missing? true}
  (subject/max-rejection [subject/reject-nil]) => {:reject-nil? true :reject-missing? false}

  (subject/max-rejection [subject/reject-nil even? subject/reject-missing])
  => {:reject-nil? true :reject-missing? true})

(fact "can remove pseudopreds from a list"
  (subject/without-pseudopreds []) => []
  (subject/without-pseudopreds [even?]) => [even?]

  (subject/without-pseudopreds [subject/required-path even? subject/reject-nil subject/reject-missing]) => [even?])
