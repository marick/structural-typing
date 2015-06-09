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

(fact "bouncer-style checks are allowed"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :hork [:a :b] {:a sequential?}))]
    (fact ":b is still required"
      (type/checked type-repo :hork {:a []}) => :failure-handler-called
      (accumulator/messages) => (just #":b must be present"))
  
    (fact ":a is still required"
      (type/checked type-repo :hork {:b 2}) => :failure-handler-called
      (accumulator/messages) => (just #"a must be present"))

    (fact "in addition, :a must be a sequential"
      (type/checked type-repo :hork {:a 1 :b 2}) => :failure-handler-called
      (accumulator/messages) => (just ["Custom validation failed for :a"]))

    (fact "it is possible to succeed"
      (type/checked type-repo :hork {:a [] :b 2}) => {:a [] :b 2})))

(fact "You can use variables instead of functions for better error-reporting"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :hork [:a :b] {:a #'sequential?}))]
    (type/checked type-repo :hork {:a 1 :b 2}) => :failure-handler-called
    (accumulator/messages) => (just [":a is not `sequential?`"])))
  

(facts "about how you do optional values: omit them from sequential"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :hork [] {:a even?}))]
    (fact "observe that a missing element causes a check of nil"
      (type/checked type-repo :hork {:b "head"}) => (throws #"Argument must be an integer"))
    
    (fact "but otherwise, checks work"
      (type/checked type-repo :hork {:a 2}) => {:a 2}
      (type/checked type-repo :hork {:a 2, :b 1}) => {:a 2, :b 1}
      
      (type/checked type-repo :hork {:a 1}) => :failure-handler-called
      (accumulator/messages) => (just "Custom validation failed for :a")))
  
  (fact "a better way of handling an optional type is type/optional"
    ( (type/optional number? even?) nil) => true
    ( (type/optional number? even?) 'a) => false  ; note evaluation short-circuits.
    ( (type/optional number? even?) 1) => false
    ( (type/optional number? even?) 2) => true

    ( (type/optional even?) "string") => false ; exceptions count as false.
    
    (let [type-repo (-> accumulator/type-repo
                        (type/named :hork [] {:a (type/optional number? even?)}))]
      (type/checked type-repo :hork {:b "head"}) => {:b "head"}

      (type/checked type-repo :hork {:a 2}) => {:a 2}
      (type/checked type-repo :hork {:a 2, :b 1}) => {:a 2, :b 1}
      
      (type/checked type-repo :hork {:a 1}) => :failure-handler-called
      (accumulator/messages) => (just "Custom validation failed for :a"))))

(fact "Message arguments are useful"
  (let [type-repo (-> accumulator/type-repo
                      (type/named :hork [] {:a [[number? :message "%s must be a number"] even?]}))]
    (type/checked type-repo :hork {:a "head"}) => :failure-handler-called
    (accumulator/messages) => (just ":a must be a number")))

(future-fact "bouncer validators work")

(future-fact "nested structures work - currently you get trees of messages published")

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


(fact 
  (#'type/custom-bouncer-descriptor even?) => [even?]
  (#'type/custom-bouncer-descriptor []) => []
  (#'type/custom-bouncer-descriptor [even?]) => [even?]

  (#'type/custom-bouncer-descriptor [[even? "msg"] odd?]) => [[even? "msg"] odd?]

  (#'type/custom-bouncer-descriptor [#'odd?]) => [[#'odd? :message "%s is not `odd?`"]]
  (#'type/custom-bouncer-descriptor #'odd?) => [[#'odd? :message "%s is not `odd?`"]]
  (#'type/custom-bouncer-descriptor [[#'odd? :message "foo"]]) => [[#'odd? :message "foo"]]

  )
