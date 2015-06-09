(ns structural-typing.f-validators
  (:require [structural-typing.type :as type]
            [structural-typing.testutil.accumulator :as accumulator]
            [structural-typing.validators :as v])
  (:use midje.sweet))


(namespace-state-changes (before :facts (accumulator/reset!)))

(fact number
  (let [type-repo (-> accumulator/type-repo
                      (type/named :hork [] {:a v/number}))]
    (type/checked type-repo :hork {:b "head"}) => {:b "head"}
    (type/checked type-repo :hork {:a "head"}) => :failure-handler-called
    (accumulator/messages) => (just #":a is `\"head\"`, which is not a number")
    
    (type/checked type-repo :hork {:a 2}) => {:a 2}
    (type/checked type-repo :hork {:a 2, :b 1}) => {:a 2, :b 1}))
      


(future-fact "own validators")    

