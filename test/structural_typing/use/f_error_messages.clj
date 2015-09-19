(ns structural-typing.use.f-error-messages
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(future-fact "There should be an error if there's an instance of `includes` in a path")
