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


(fact "modifying the behavior of predicates"
  (fact "`message` adds a message to the predicate's metadata"
    (let [pred (with-meta (fn [x] false) {:gorp true})
          result (-> pred (type/message "msg"))]
      (result 1) => false
      (meta result) => (contains {:gorp true, :default-message-format "msg"})))
  

  (fact "`only-when` adds a guard"
    (let [result (-> even? (type/only-when pos?))]
      (result -1) => true
      (result -2) => true
      (result 1) => false
      (result 2) => true))

  (fact "`only-when` preserves metadata"
    (let [result (-> even? (type/message "foo") (type/only-when pos?))]
      (result -1) => true
      (result -2) => true
      (result 1) => false
      (result 2) => true
      (meta result) => (contains {:default-message-format "foo"}))))
    


(future-fact "type/all-of and type/some-of")
(future-fact "double-check validators and extra args")
