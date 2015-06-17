(ns structural-typing.example-details.det-single-type-repository
  "An example of using the global type repository that's available in all namespaces"
  (:require [structural-typing.global-type :as global-type])
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

;; (global-type/start-over!)
;; (global-type/set-failure-handler! accumulator/failure-handler) ; stash failures in an atom
;; (namespace-state-changes (before :facts (accumulator/reset!)))

;; (global-type/named! :stringish ["not a keyword"])
;; (global-type/named! :even [:a] {:a even?})

;; (fact "simple checking"
;;   (type/checked :stringish {"not a keyword" 1}) => {"not a keyword" 1})

;; (fact "value checking"
;;   (type/checked :even {:a 2}) => {:a 2})
  
;; (fact "instance?"
;;   (type/instance? :stringish {"not a keyword" 1}) => true
;;   (type/instance? :stringish {:a 1}) => false)
  
;; (fact "checking"
;;   (type/checked :stringish {:a 1}) => :failure-handler-called
;;   (accumulator/messages) => (just #"\"not a keyword\" must be present"))

;; (fact "coercion"
;;   (global-type/coercion! :stringish (fn [from]
;;                                       (set/rename-keys from {:not-a-keyword "not a keyword"})))
;;   (type/coerced :stringish {:a 1}) => :failure-handler-called
;;   (type/coerced :stringish {"not a keyword" 1}) => {"not a keyword" 1}
;;   (type/coerced :stringish {:not-a-keyword 1}) => {"not a keyword" 1})


;; (global-type/start-over!)

