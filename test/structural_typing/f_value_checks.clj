(ns structural-typing.f-value-checks
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

(namespace-state-changes (before :facts (accumulator/reset!)))

(fact "adding an optional predicate"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :hork [:a :b] {:a sequential?}))]

    (type/checked type-repo :hork {:a [] :b 2}) => {:a [] :b 2}

    (fact ":b is still required"
      (type/checked type-repo :hork {:a []}) => :failure-handler-called
      (accumulator/messages) => (just #":b must be present"))
  
    (fact ":a is still required"
      (type/checked type-repo :hork {:b 2}) => :failure-handler-called
      (accumulator/messages) => (just #"a must be present"))

    (fact "in addition, :a must be a sequential"
      (type/checked type-repo :hork {:a 1 :b 2}) => :failure-handler-called
      (accumulator/messages) => (just ["Custom validation failed for :a"]))))

(fact "multiple predicates can be enclosed in a list"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :hork [:a :b] {:a [sequential?]}))]
    (type/checked type-repo :hork {:a 1 :b 2}) => :failure-handler-called
    (accumulator/messages) => (just ["Custom validation failed for :a"])))

(fact "Messages can be specified alongside a predicate"
  (let [a-number [number? :message "%s must be a number"]
        type-repo (-> accumulator/type-repo
                      (type/named :hork [] {:a [a-number even?]}))]
    (type/checked type-repo :hork {:a "head"}) => :failure-handler-called
    (accumulator/messages) => (just ":a must be a number")))

(fact "You can use variables instead of functions for better automatic error-reporting"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :hork [:a :b] {:a #'sequential?}))]
    (type/checked type-repo :hork {:a 1 :b 2}) => :failure-handler-called
    (accumulator/messages) => (just [":a should be `sequential?`; it is `1`"])))
  

(facts "you check the value of optional keys by omitting the key from the vector"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :hork [] {:a #'even?}))]
    (type/checked type-repo :hork {:a 2}) => {:a 2}
    (type/checked type-repo :hork {:a 2, :b 1}) => {:a 2, :b 1}
      
    (type/checked type-repo :hork {:a 1}) => :failure-handler-called
    (accumulator/messages) => (just ":a should be `even?`; it is `1`")

    (fact "observe that a missing element does NOT cause a check"
      (type/checked type-repo :hork {:b "head"}) =not=> (throws #"Argument must be an integer"))))
    


(fact "bouncer validators work"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :hork [:a] {:a bouncer.validators/number}))]
    (type/checked type-repo :hork {:a "foo"}) => :failure-handler-called
    (accumulator/messages) => (just ":a must be a number")))

(future-fact "nested structures work - currently you get trees of messages published")
