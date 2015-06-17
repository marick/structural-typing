(ns type-checking-values
  "You can type-check the values of maps, not just the keys"
  (:require [structural-typing.type :as type]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

;; (namespace-state-changes (before :facts (accumulator/reset!)))

;; ;; We start with a test support repo. You'd normally use `type/empty-type-repo`.
;; (def type-repo (-> accumulator/type-repo
;;                    (type/named :using-a-plain-function [:a :b] {:a sequential?})
;;                    (type/named :using-a-var [:a :b] {:a #'sequential?})
;;                    (type/named :multiple-predicates [:a :b] {:a [#'pos? #'even?]})
;;                    (type/named :any-truthy-function [:a] {:a [(partial some #{"a match"})]})))

;; (def type-checked (partial type/checked type-repo))

;; (fact "You can use any predicate to check the value of a required key"
;;   ;; ... but the error message is not great
;;   (type-checked :using-a-plain-function {:a 1 :b 2}) => :failure-handler-called
;;   (accumulator/messages) => ["Custom validation failed for :a"])

;; (fact "An easy way to get a better error message is to use a var for the predicate"
;;   (type-checked :using-a-var {:a 1 :b 2}) => :failure-handler-called
;;   (accumulator/messages) => [":a should be `sequential?`; it is `1`"])

;; (fact "You can use multiple predicates"
;;   (type-checked :multiple-predicates {:a 1 :b 2}) => :failure-handler-called
;;   (accumulator/messages) => [":a should be `even?`; it is `1`"])

;; (fact "Note that a failure in one predicate means the remainder are not called"
;;   (type-checked :multiple-predicates {:a -1 :b 2}) => :failure-handler-called
;;   ;; No error message about even
;;   (accumulator/messages) => [":a should be `pos?`; it is `-1`"])
  
;; (fact "predicates don't have to return strictly true or false"
;;   (type-checked :any-truthy-function {:a ["a match"]}) => {:a ["a match"]}

;;   (type-checked :any-truthy-function {:a ["no match"]}) => :failure-handler-called
;;   (accumulator/messages) => ["Custom validation failed for :a"])

;; (fact "The above message is annoying, but you can also add custom messages, documented elsewhere.")
