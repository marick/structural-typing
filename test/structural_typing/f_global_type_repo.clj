(ns structural-typing.f-global-type-repo
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

(namespace-state-changes (before :facts (accumulator/reset!)))

;;; Global type repo

(fact "in most uses, a global variable stores all the types"
  (type/start-over!)
  (type/set-failure-handler! accumulator/failure-handler)

  (fact "ordinary checking"
    (type/named! :stringish ["not a keyword"])
    (type/checked :stringish {"not a keyword" 1}) => {"not a keyword" 1}

    (fact "instance?"
      (type/instance? :stringish {"not a keyword" 1}) => true
      (type/instance? :stringish {:a 1}) => false)

    (fact "checking"
      (type/checked :stringish {:a 1}) => :failure-handler-called
      (accumulator/messages) => (just #"\"not a keyword\" must be present"))

    (fact "coercion"
      (type/coercion! :stringish (fn [from]
                                 (set/rename-keys from {:not-a-keyword "not a keyword"})))
      (type/coerce :stringish {:a 1}) => :failure-handler-called
      (type/coerce :stringish {"not a keyword" 1}) => {"not a keyword" 1}
      (type/coerce :stringish {:not-a-keyword 1}) => {"not a keyword" 1})))

