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
      @accumulator => (just #":b must be present"))
    
    (fact "does not stop with one error"
      (type/checked type-repo :hork {:c 3}) => :failure-handler-called
      @accumulator => (just [#":a must be present" #":b must be present"]
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
      @accumulator => (just #"a must be present")))
  (fact "note that it's OK for there to be no type check for a coercion result."
    (let [type-repo (-> type/empty-type-repo
                        (type/coercion :hork (fn [from] (set/rename-keys from {:a :bbb}))))]
      (type/coerce type-repo :hork {:a 1, :b 2}) => {:bbb 1, :b 2})))

(fact "bouncer-style checks are allowed"
  (let [type-repo (-> accumulating-type-repo
                      (type/named :hork [:a :b] {:a sequential?}))]
    (fact ":b is still required"
      (type/checked type-repo :hork {:a []}) => :failure-handler-called
      @accumulator => (just #":b must be present"))
  
    (fact ":a is still required"
      (type/checked type-repo :hork {:b 2}) => :failure-handler-called
      @accumulator => (just #"a must be present"))

    (fact "in addition, :a must be a sequential"
      (type/checked type-repo :hork {:a 1 :b 2}) => :failure-handler-called
      @accumulator => (just ["Custom validation failed for :a"]))

    (fact "it is possible to succeed"
      (type/checked type-repo :hork {:a [] :b 2}) => {:a [] :b 2})))

(facts "about how you do optional values: omit them from sequential"
  (let [type-repo (-> accumulating-type-repo
                      (type/named :hork [] {:a even?}))]
    (fact "observe that a missing element causes a check of nil"
      (type/checked type-repo :hork {:b "head"}) => (throws #"Argument must be an integer"))
    
    (fact "but otherwise, checks work"
      (type/checked type-repo :hork {:a 2}) => {:a 2}
      (type/checked type-repo :hork {:a 2, :b 1}) => {:a 2, :b 1}
      
      (type/checked type-repo :hork {:a 1}) => :failure-handler-called
      @accumulator => (just "Custom validation failed for :a")))
  
  (fact "a better way of handling an optional type is type/optional"
    ( (type/optional number? even?) nil) => true
    ( (type/optional number? even?) 'a) => false  ; note evaluation short-circuits.
    ( (type/optional number? even?) 1) => false
    ( (type/optional number? even?) 2) => true

    ( (type/optional even?) "string") => false ; exceptions count as false.
    
    (let [type-repo (-> accumulating-type-repo
                        (type/named :hork [] {:a (type/optional number? even?)}))]
      (type/checked type-repo :hork {:b "head"}) => {:b "head"}

      (type/checked type-repo :hork {:a 2}) => {:a 2}
      (type/checked type-repo :hork {:a 2, :b 1}) => {:a 2, :b 1}
      
      (type/checked type-repo :hork {:a 1}) => :failure-handler-called
      @accumulator => (just "Custom validation failed for :a"))))

(fact "Message arguments are useful"
  (let [type-repo (-> accumulating-type-repo
                      (type/named :hork [] {:a [[number? :message "%s must be a number"] even?]}))]
    (type/checked type-repo :hork {:a "head"}) => :failure-handler-called
    @accumulator => (just ":a must be a number")))

(future-fact "bouncer validators work")

(future-fact "own validators")    

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
      @accumulator => (just #"\"not a keyword\" must be present"))

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
      @accumulator => (just {:a [":a must be present and non-nil"]
                             :b [":b must be present and non-nil"]}
                            {:c 3})))
(facts "about `better-messages`"
  (fact "a typical call formats keys and values, uses default-message-format"
    (type/better-messages {:path [:a]
                           :value "wrong"
                           :metadata {:default-message-format "%s - %s"}})
    => ":a - \"wrong\"")

  (fact "a non-singular path is printed as an array"
    (type/better-messages {:path [:a, :b]
                           :value "wrong"
                           :metadata {:default-message-format "%s - %s"}})
    => "[:a :b] - \"wrong\"")

  (fact "a message-format overrides the default"
    (type/better-messages {:path [:a, :b]
                           :value "wrong"
                           :metadata {:default-message-format "%s - %s"}
                           :message "%s derp %s"})
    => "[:a :b] derp \"wrong\"")

  (fact "a single format argument is allowed in a message"
    (type/better-messages {:path [:a]
                           :value "wrong"
                           :metadata {:default-message-format "%s must be present"}})
    => ":a must be present")

  (fact "the default message format can be a function that takes the bouncer map"
    (type/better-messages {:path ["a"]
                           :value 3
                           :metadata {:default-message-format
                                      #(format "%s/%s" (:path %) (inc (:value %)))}})
    => "[\"a\"]/4")
    
  (fact "... as can be the given message format (which is given raw path and value)"
    (type/better-messages {:path ["a"]
                           :value 3
                           :metadata {:default-message-format
                                      #(format "%s/%s" (:path %) (inc (:value %)))}})
    => "[\"a\"]/4")
    
  )
