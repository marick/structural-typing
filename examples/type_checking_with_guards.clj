(ns type-checking-with-guards
  "Predicates used to check the values of keys can be guarded by other predicates."
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

;; ;; We start with a test support repo. You'd normally use `type/empty-type-repo`.
;; (def type-repo (-> accumulator/type-repo
;;                    (type/named :positive-even [:a]
;;                                {:a [#'integer? (-> #'even? (type/only-when pos?))]})
;;                    (type/named :positive-even-if-present [] ; a not required
;;                                {:a [#'integer? (-> #'even? (type/only-when pos?))]})))

;; (def type-checked (partial type/checked type-repo))

;; (namespace-state-changes (before :facts (accumulator/reset!)))

;; (fact "both variants complain about positive odd number"
;;   (type-checked :positive-even {:a 1}) => :failure-handler-called
;;   (accumulator/messages) => [":a should be `even?`; it is `1`"]
  
;;   (type-checked :positive-even-if-present {:a 1}) => :failure-handler-called
;;   (accumulator/messages) => [":a should be `even?`; it is `1`"])
  
;; (fact "because of the guard, neither complains about the negative odd number"
;;   (type-checked :positive-even {:a -1}) => {:a -1}
;;   (type-checked :positive-even-if-present {:a -1}) => {:a -1})

;; (fact "both accept positive even"
;;   (type-checked :positive-even {:a 2}) => {:a 2}
;;   (type-checked :positive-even-if-present {:a 2}) => {:a 2})

;; (fact "only the first requires `:a` to be present"
;;   (type-checked :positive-even {}) => :failure-handler-called
;;   (accumulator/messages) => [":a must be present and non-nil"]

;;   (type-checked :positive-even-if-present {}) => {})

;; (fact "both reject non-numbers"
;;   (type-checked :positive-even {:a :not-integer}) => :failure-handler-called
;;   (accumulator/messages) => [":a should be `integer?`; it is `:not-integer`"]
  
;;   (type-checked :positive-even-if-present {:a :not-integer}) => :failure-handler-called
;;   (accumulator/messages) => [":a should be `integer?`; it is `:not-integer`"])
