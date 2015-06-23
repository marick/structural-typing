(ns structural-typing.api.f-type-repo
  (:require [structural-typing.api.type-repo :as subject]
            [structural-typing.api.path :as path]
            [structural-typing.api.defaults :as default])
  (:use midje.sweet))

(fact "an example case (most are in the examples dir"
  (let [repo (-> (subject/->TypeRepo identity default/explanations)
                 (subject/hold-type :Type [ [:a] ]))]
    (subject/check-type repo :Type {:b 1}) => (just ":a must exist and be non-nil")
    (subject/check-type repo :Type {:a 1}) => {:a 1}))

(fact "there is an empty type repo"
  (let [repo (-> subject/empty-type-repo
                 (subject/hold-type :Type [ {:a integer?} ])
                 (subject/replace-error-handler default/explanations))]
    (subject/check-type repo :Type {:b 1}) => {:b 1}
    (subject/check-type repo :Type {:a 1}) => {:a 1}
    (subject/check-type repo :Type {:a "string"}) => (just ":a should be `integer?`; it is `\"string\"`")))
    
(fact "using previously-defined types"
  (let [repo (-> subject/empty-type-repo
                 (subject/hold-type :A [ {:a integer?} ])
                 (subject/hold-type :AB [ (path/an :A) [:b] {:b string?} ])
                 (subject/replace-error-handler default/explanations))]
    (subject/check-type repo :AB {:b "s"}) => {:b "s"}
    (subject/check-type repo :AB {:a 1 :b "s"}) => {:a 1 :b "s"}
    (subject/check-type repo :AB {:a "s"}) => (just ":a should be `integer?`; it is `\"s\"`"
                                                   ":b must exist and be non-nil"
                                                   :in-any-order)))
