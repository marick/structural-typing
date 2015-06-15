(ns custom-messages
  "How to make your own custom messages"
  (:require [structural-typing.type :as type]
            [structural-typing.global-type :as global-type]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

(namespace-state-changes (before :facts (accumulator/reset!)))

(fact "Predicates can be annotated with strings with zero, one, or two format descriptors"
  (let [zero (-> number? (type/message "0"))
        one (-> number? (type/message "%s 1"))
        two (-> number? (type/message "%s 2 %s"))
        type-repo (-> accumulator/type-repo
                      (type/named :some-type [] {:a zero
                                                 :b one
                                                 :c two}))]
    (type/checked type-repo :some-type {:a :bad-a
                                        :b :bad-b
                                        :c :bad-c}) => :failure-handler-called
    (accumulator/messages) => (just "0" ":b 1" ":c 2 :bad-c" :in-any-order)))


(fact "Predicates can also be annotated with functions that generate messages"
  (let [message-gen (fn [{:keys [path value]}]
                      (format "%s should be a square root of %s." path (* value value)))
        type-repo (-> accumulator/type-repo
                     (type/named :ok [:x] {:x [(-> even? (type/message message-gen))]}))]
    
    (type/checked type-repo :ok {:x 9}) => :failure-handler-called
    (accumulator/messages) => ["[:x] should be a square root of 81."]))
