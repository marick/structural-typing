(ns type-checking-required-keys
  "The most basic behavior is to check that particular keys exist."
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))
  
;; We start with a test support repo. You'd normally use `type/empty-type-repo`.
(def type-repo (-> accumulator/type-repo 
                   (type/named :requires-a-and-b [:a :b])
                   (type/named :requires-a-b-c [:a :b :c])))

(def type-checked (partial type/checked type-repo))
(def type-instance? (partial type/instance? type-repo))
                

(namespace-state-changes (before :facts (accumulator/reset!)))

(fact "type checking: keys only, no nested maps, no checking of values"
  (fact "normally returns input map"
    (type-checked :requires-a-and-b {:a 1 :b 2})
      => {:a 1 :b 2})
    
    (fact "extra keys are fine"
      (type-checked :requires-a-and-b {:a 1 :b 2 :extra 3})
      => {:a 1 :b 2 :extra 3})
    
    (fact "invokes the failure handler on formatted values"
      (type-checked :requires-a-and-b {:a 3}) => :failure-handler-called
      (accumulator/messages) => (just #":b must be present"))
    
    (fact "does not stop with one erroneous key"
      (type-checked :requires-a-and-b {:c 3}) => :failure-handler-called
      (accumulator/messages) => (just [#":a must be present" #":b must be present"]
                                      :in-any-order)))

(fact "`instance?` is a predicate for checking types"
  (let [type-repo (type/named accumulator/type-repo :requires-a-and-b [:a :b])]
    (type-instance? :requires-a-and-b {:a 1}) => false
    (type-instance? :requires-a-and-b {:a 1 :b 2}) => true
    (type-instance? :requires-a-and-b {:a 1 :b 2 :c 3}) => true))


(defrecord R [a b])
(fact "works on records too"
  (let [type-repo (type/named accumulator/type-repo :requires-a-b-c [:a :b :c])]
    (type-checked :requires-a-b-c (assoc (->R 1 2) :c 3)) => (just {:a 1, :b 2, :c 3})

    (type-checked :requires-a-b-c (->R 1 2)) => :failure-handler-called
    (accumulator/messages) => [":c must be present and non-nil"]))
  
