(ns structural-typing.f-validators
  (:require [structural-typing.type :as type]
            [structural-typing.validators :as v])
  (:use midje.sweet))

(def accumulator (atom []))
(defn accumulating-failure-handler [o]
  (swap! accumulator (constantly o))
  :failure-handler-called)

(def accumulating-type-repo
  (-> type/empty-type-repo (assoc :failure-handler accumulating-failure-handler)))

(namespace-state-changes (before :facts (reset! accumulator [])))

(fact number
  (let [type-repo (-> accumulating-type-repo
                      (type/named :hork [] {:a v/number}))]
    (type/checked type-repo :hork {:b "head"}) => {:b "head"}
    (type/checked type-repo :hork {:a "head"}) => :failure-handler-called
    @accumulator => (just #":a is `\"head\"`, which is not a number")
    
    (type/checked type-repo :hork {:a 2}) => {:a 2}
    (type/checked type-repo :hork {:a 2, :b 1}) => {:a 2, :b 1}))
      

