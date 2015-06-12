(ns structural-typing.f-tbd
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))


(namespace-state-changes (before :facts (accumulator/reset!)))



;;; Util

(future-fact "the bouncer map adapter can be customized"
  (let [type-repo (-> accumulator/type-repo
                      (assoc ;; this will cause the maps to be emitted separately.
                             :map-adapter (fn [e kvs] (vector e kvs)))
                      (type/named :hork [:a :b]))]
      (type/checked type-repo :hork {:c 3}) => :failure-handler-called
      (accumulator/messages) => (just {:a [":a must be present and non-nil"]
                                       :b [":b must be present and non-nil"]}
                                      {:c 3})))


(facts "about validators that appear in optional maps"
  ; 1. Perhaps custom messages
  ; 2. Definitely marked as optional.
  ; 3. Exceptions are treated as `false`.
  (let [subject #'type/forgiving-optional-validator]

    (fact "plain predicates"
      (let [new-even? (subject even?)]
        (new-even? 1) => false
        (new-even? 2) => true
        (even? "foo") => (throws)
        (new-even? "foo") => false
        
        (meta new-even?) => (contains {:optional true})))

          
    (fact "plain predicates retain existing metadata"
      (let [new-even? (subject (with-meta even? {:default-message-format "derp"}))]
        (meta new-even?) => (contains {:optional true
                                       :default-message-format "derp"})))

          
    (fact "vars"
      (let [new-even? (subject #'even?)]
        (new-even? 1) => false
        (new-even? 2) => true
        (even? "foo") => (throws)
        (new-even? "foo") => false
        
        (meta new-even?) => (contains {:optional true
                                       :default-message-format "%s should be `even?`; it is `%s`"})))

    (fact "vectors with messages"
      (let [new-even? (subject [even? :message "derp!"])]
        (new-even? 1) => false
        (new-even? 2) => true
        (even? "foo") => (throws)
        (new-even? "foo") => false
        
        (meta new-even?) => (contains {:optional true
                                       :default-message-format "derp!"})))))






(fact "bouncer validators work"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :some-type [:a] {:a bouncer.validators/number}))]
    (type/checked type-repo :some-type {:a "foo"}) => :failure-handler-called
    (accumulator/messages) => (just ":a must be a number")))

(future-fact "nested structures work - currently you get trees of messages published")
