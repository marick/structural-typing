(ns structural-typing.example-details.det-type-checking-values
  "You can type-check the values of maps, not just the keys"
  (:require [structural-typing.type :as type]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

;; (namespace-state-changes (before :facts (accumulator/reset!)))

;; (fact "adding an optional predicate"
;;   (let [type-repo (-> accumulator/type-repo
;;                       (type/named :some-type [:a :b] {:a sequential?}))]

;;     (fact ":b is still required"
;;       (type/checked type-repo :some-type {:a []}) => :failure-handler-called
;;       (accumulator/messages) => (just #":b must be present"))
  
;;     (fact ":a is still required"
;;       (type/checked type-repo :some-type {:b 2}) => :failure-handler-called
;;       (accumulator/messages) => (just #"a must be present"))

;;     (fact "in addition, :a must be a sequential"
;;       (type/checked type-repo :some-type {:a 1 :b 2}) => :failure-handler-called
;;       (accumulator/messages) => (just ["Custom validation failed for :a"]))

;;     (fact "and here's the success case"
;;       (type/checked type-repo :some-type {:a [] :b 2}) => {:a [] :b 2})))

;; (fact "multiple predicates can be enclosed in a list"
;;   (let [type-repo (-> accumulator/type-repo
;;                       (type/named :some-type [:a :b] {:a [coll? sequential?]}))]
;;     (type/checked type-repo :some-type {:a 1 :b 2}) => :failure-handler-called
;;     (accumulator/messages) => (just ["Custom validation failed for :a"])))

;; (fact "You can use variables instead of functions for better automatic error-reporting"
;;   (let [type-repo (-> accumulator/type-repo
;;                       (type/named :some-type [:a :b] {:a [#'coll? #'sequential?]}))]
;;     (type/checked type-repo :some-type {:a 1 :b 2}) => :failure-handler-called
;;     (accumulator/messages) => (just [":a should be `coll?`; it is `1`"])))
  




