(ns optional-keys-with-values
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

(namespace-state-changes (before :facts (accumulator/reset!)))

(facts "you check the value of optional keys by omitting the key from the vector"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :some-type [] {:a #'even?}))]
    (type/checked type-repo :some-type {:a 2}) => {:a 2}
    (type/checked type-repo :some-type {:a 2, :b 1}) => {:a 2, :b 1}
      
    (type/checked type-repo :some-type {:a 1}) => :failure-handler-called
    (accumulator/messages) => [":a should be `even?`; it is `1`"]

    (fact "observe that a missing element does NOT cause a check"
      (type/checked type-repo :some-type {:b "head"}) =not=> (throws #"Argument must be an integer"))))

(fact "predicates can be guarded - and ignored if the guard is false"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :positive-even [:a]
                                  {:a [#'integer? (-> #'even? (type/only-when pos?))]}))]
    (type/checked type-repo :positive-even {}) => :failure-handler-called
    (accumulator/messages) => [":a must be present and non-nil"]

    (type/checked type-repo :positive-even {:a :not-integer}) => :failure-handler-called
    (accumulator/messages) => [":a should be `integer?`; it is `:not-integer`"]

    (type/checked type-repo :positive-even {:a 1}) => :failure-handler-called
    (accumulator/messages) => [":a should be `even?`; it is `1`"]

    (type/checked type-repo :positive-even {:a 2}) => {:a 2}

    ;; The guard prevents a failure
    (type/checked type-repo :positive-even {:a -1}) => {:a -1}))


(fact "the guarded predicate can be for an optional value"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :positive-even [] ; a not required
                                  {:a [#'integer? (-> #'even? (type/only-when pos?))]}))]
    (type/checked type-repo :positive-even {}) => {}
    (accumulator/messages) =not=> [":a must be present and non-nil"]

    (type/checked type-repo :positive-even {:a :not-integer}) => :failure-handler-called
    (accumulator/messages) => [":a should be `integer?`; it is `:not-integer`"]

    (type/checked type-repo :positive-even {:a 1}) => :failure-handler-called
    (accumulator/messages) => [":a should be `even?`; it is `1`"]

    (type/checked type-repo :positive-even {:a 2}) => {:a 2}

    ;; The guard prevents a failure
    (type/checked type-repo :positive-even {:a -1}) => {:a -1}))



(fact "note that `message` and `only-when` can be combined"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :positive-even [:a]
                                  {:a [#'integer? (-> even?
                                                      (type/only-when pos?)
                                                      (type/message "A positive `%s` should be even."))]}))]
    (type/checked type-repo :positive-even {}) => :failure-handler-called
    (accumulator/messages) => [":a must be present and non-nil"]

    (type/checked type-repo :positive-even {:a :not-integer}) => :failure-handler-called
    (accumulator/messages) => [":a should be `integer?`; it is `:not-integer`"]

    (type/checked type-repo :positive-even {:a 1}) => :failure-handler-called
    (accumulator/messages) => ["A positive `:a` should be even."]

    (type/checked type-repo :positive-even {:a 2}) => {:a 2}

    ;; The guard prevents a failure
    (type/checked type-repo :positive-even {:a -1}) => {:a -1}))

(fact "just to be squeaky-sure, combine them in the other order"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :positive-even [:a]
                                  {:a [#'integer? (-> even?
                                                      (type/message "A positive `%s` should be even.")
                                                      (type/only-when pos?))]}))]
    (type/checked type-repo :positive-even {}) => :failure-handler-called
    (accumulator/messages) => [":a must be present and non-nil"]

    (type/checked type-repo :positive-even {:a :not-integer}) => :failure-handler-called
    (accumulator/messages) => [":a should be `integer?`; it is `:not-integer`"]

    (type/checked type-repo :positive-even {:a 1}) => :failure-handler-called
    (accumulator/messages) => ["A positive `:a` should be even."]

    (type/checked type-repo :positive-even {:a 2}) => {:a 2}

    ;; The guard prevents a failure
    (type/checked type-repo :positive-even {:a -1}) => {:a -1}))
