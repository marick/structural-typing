(ns single-type-repository
  "An example of using the global type repository that's available in all namespaces"
  (:require [structural-typing.global-type :as global-type])
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

(global-type/start-over!)
(global-type/set-failure-handler! accumulator/failure-handler) ; stash failures in an atom
(namespace-state-changes (before :facts (accumulator/reset!)))

(global-type/named! :exists [:a :b])
(global-type/named! :even [:a] {:a even?})

(fact "key existence"
  (type/checked :exists {:a 1, :b 1}) => {:a 1, :b 1})

(fact "value checking"
  (type/checked :even {:a 2}) => {:a 2})
  
(fact "instance?"
  (type/instance? :exists {:a 1 :b 1}) => true
  (type/instance? :exists {:a 1}) => false)
  
(fact "failures"
  (type/checked :exists {:a 1}) => :failure-handler-called
  (accumulator/messages) => [":b must be present and non-nil"])

(fact "coercion"
  (global-type/coercion! :exists (fn [from]
                                 (set/rename-keys from {:null-a :a})))
  (type/coerced :exists {:null-a 1}) => :failure-handler-called
  (accumulator/messages) => [":b must be present and non-nil"]

  (type/coerced :exists {:a 1, :b 2}) => {:a 1, :b 2}
  (type/coerced :exists {:null-a 1, :b 2}) => {:a 1, :b 2})

(global-type/start-over!)

