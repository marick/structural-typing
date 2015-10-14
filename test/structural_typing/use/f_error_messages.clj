(ns structural-typing.use.f-error-messages
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(future-fact "There should be an error if there's an instance of `includes` in a path")

(future-fact "error out if you use `requires` on the right-hand side"
  (type! :Line {(each-of :head :tail) (requires :x :y)})
  )


(future-fact "reject impossible condensed type descriptions"
  (type! :X 1)
  ;; vector is an old-style description
  ;; other kinds of seqs?
  ;; records?
  )

