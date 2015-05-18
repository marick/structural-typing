(ns structural-typing.f-type
  (:require [structural-typing.type :as type]
            [clojure.set :as set])
  (:use midje.sweet))

(def accumulator (atom []))
(defn accumulating-failure-handler [o]
  (swap! accumulator (constantly o))
  :failure-handler-called)

(def accumulating-type-repo
  (-> type/empty-type-repo (assoc :failure-handler accumulating-failure-handler)))

(namespace-state-changes (before :facts (reset! accumulator [])))

(type/start-over!)

(fact "basic stateless type checking"
  (let [type-repo (-> accumulating-type-repo
                      (type/named :hork [:a :b]))]
    (fact "normally returns input map"
      (type/checked type-repo :hork {:a 1 :b 2})
      => {:a 1 :b 2})
    
    (fact "extra keys are fine"
      (type/checked type-repo :hork {:a 1 :b 2 :extra 3})
      => {:a 1 :b 2 :extra 3})
    
    (fact "invokes the failure handler on formatted values"
      (type/checked type-repo :hork {:a 3}) => :failure-handler-called
      @accumulator => ["b must be present"])
    
    (fact "does not stop with one error"
      (type/checked type-repo :hork {:c 3}) => :failure-handler-called
      @accumulator => (just ["a must be present" "b must be present"]
                            :in-any-order))))

(fact "`instance?` is a predicate for checking types"
  (let [type-repo (type/named accumulating-type-repo :hork [:a :b])]
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
    (let [type-repo (-> accumulating-type-repo
                        (type/named :hork [:a :b])
                        (type/coercion :hork (fn [from] (set/rename-keys from {:a :aa}))))]
      (type/coerce type-repo :hork {:a 1, :b 2}) => :failure-handler-called
      @accumulator => ["a must be present"]))
  (fact "note that it's OK for there to be no type check for a coercion result."
    (let [type-repo (-> type/empty-type-repo
                        (type/coercion :hork (fn [from] (set/rename-keys from {:a :bbb}))))]
      (type/coerce type-repo :hork {:a 1, :b 2}) => {:bbb 1, :b 2})))

;;; Global type repo

(fact "normally, a global variable stores all the types"
  (type/start-over!)
  (type/set-failure-handler! accumulating-failure-handler)

  (fact "ordinary checking"
    (type/named! :stringish ["not a keyword"])
    (type/checked :stringish {"not a keyword" 1}) => {"not a keyword" 1}

    (fact "instance?"
      (type/instance? :stringish {"not a keyword" 1}) => true
      (type/instance? :stringish {:a 1}) => false)

    (fact "checking"
      (type/checked :stringish {:a 1}) => :failure-handler-called
      @accumulator => ["not a keyword must be present"])

    (fact "coercion"
      (type/coercion! :stringish (fn [from]
                                 (set/rename-keys from {:not-a-keyword "not a keyword"})))
      (type/coerce :stringish {:a 1}) => :failure-handler-called
      (type/coerce :stringish {"not a keyword" 1}) => {"not a keyword" 1}
      (type/coerce :stringish {:not-a-keyword 1}) => {"not a keyword" 1})))


;;; Util
  
(fact "the default formatter is given two arguments"
  (let [type-repo (-> accumulating-type-repo
                      (assoc ;; this will cause the maps to be emitted separately.
                             :formatter (fn [e kvs] (vector e kvs)))
                      (type/named :hork [:a :b]))]
      (type/checked type-repo :hork {:c 3}) => :failure-handler-called
      @accumulator => (just {:a ["a must be present"]
                             :b ["b must be present"]}
                            {:c 3})))
