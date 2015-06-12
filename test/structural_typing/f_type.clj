(ns structural-typing.f-type
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

(namespace-state-changes (before :facts (accumulator/reset!)))

(fact "basic stateless type checking"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :hork [:a :b]))]
    (fact "normally returns input map"
      (type/checked type-repo :hork {:a 1 :b 2})
      => {:a 1 :b 2})
    
    (fact "extra keys are fine"
      (type/checked type-repo :hork {:a 1 :b 2 :extra 3})
      => {:a 1 :b 2 :extra 3})
    
    (fact "invokes the failure handler on formatted values"
      (type/checked type-repo :hork {:a 3}) => :failure-handler-called
      (accumulator/messages) => (just #":b must be present"))
    
    (fact "does not stop with one error"
      (type/checked type-repo :hork {:c 3}) => :failure-handler-called
      (accumulator/messages) => (just [#":a must be present" #":b must be present"]
                            :in-any-order))))

(fact "`instance?` is a predicate for checking types"
  (let [type-repo (type/named accumulator/type-repo :hork [:a :b])]
    (type/instance? type-repo :hork {:a 1}) => false
    (type/instance? type-repo :hork {:a 1 :b 2}) => true
    (type/instance? type-repo :hork {:a 1 :b 2 :c 3}) => true))

(fact "coercion functions"
  (fact "basic use"
    (let [type-repo (-> type/empty-type-repo
                        (type/named :hork [:a :b])
                        (type/coercion :hork (fn [from] (set/rename-keys from {:aaaa :a}))))]
      (type/coerce type-repo :hork {:aaaa 1, :b 2}) => {:a 1, :b 2}))
  (fact "the result is itself checked"
    (let [type-repo (-> accumulator/type-repo
                        (type/named :hork [:a :b])
                        (type/coercion :hork (fn [from] (set/rename-keys from {:a :aa}))))]
      (type/coerce type-repo :hork {:a 1, :b 2}) => :failure-handler-called
      (accumulator/messages) => (just #"a must be present")))
  (fact "note that it's OK for there to be no type check for a coercion result."
    (let [type-repo (-> type/empty-type-repo
                        (type/coercion :hork (fn [from] (set/rename-keys from {:a :bbb}))))]
      (type/coerce type-repo :hork {:a 1, :b 2}) => {:bbb 1, :b 2})))


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
      
(future-fact "confirm defrecord works")
