(ns structural-typing.use.f-error-messages
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(future-fact "There should be an error if there's an instance of `includes` in a path")


(fact "You need to ahve an actual type description"
  (type! :X) => (throws "You must have at least one condensed type description."))

(future-fact "reject impossible condensed type descriptions"
  (type! :X 1)
  ;; vector is an old-style description
  ;; other kinds of seqs?
  ;; records?
  )

