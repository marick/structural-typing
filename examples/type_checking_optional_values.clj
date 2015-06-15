(ns type-checking-optional-values
  "Optional keys can have their values (if any) checked by a predicate."
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

(namespace-state-changes (before :facts (accumulator/reset!)))

(facts "you check the value of optional keys by omitting the key from the vector"
  (let [type-repo (-> accumulator/type-repo
                   (type/named :even []
                               {:a #'even?}))]
    (type/checked type-repo :even {:a 2, :b 1}) => {:a 2, :b 1}
      
    (type/checked type-repo :even {:a 1, :b 2}) => :failure-handler-called
    (accumulator/messages) => [":a should be `even?`; it is `1`"]

    (fact "observe that a missing element does NOT cause a check"
      (type/checked type-repo :even {:b "head"}) =not=> (throws #"Argument must be an integer"))))
