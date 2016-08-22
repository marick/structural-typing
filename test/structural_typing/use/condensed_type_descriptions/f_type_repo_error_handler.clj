`(ns structural-typing.use.condensed-type-descriptions.f-type-repo-error-handler
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil)
  (:refer-clojure :except [any?]))


(start-over!)

(fact "normal error reporting"
  (type! :Point {:x integer? :y integer?})
  (check-for-explanations :Point {:x "one" :y "two"})
  => (just (err:shouldbe :x "integer?" "\"one\"")
           (err:shouldbe :y "integer?" "\"two\"")))

(fact "the throwing error handler"
  (on-error! throwing-error-handler)
  (built-like :Point {:x "one" :y "two"}) => (throws))

 
(fact "Default error handler returns nil"
  (on-error! default-error-handler)
  (with-out-str 
    (some-> (built-like :Point {:x "one" :y "two"})
            (prn "successful stuff")))
  =not=> #"successful")

(fact "default success handler returns the original value"
  (built-like :Point {:x 1 :y 2}) => {:x 1 :y 2})

(start-over!)
