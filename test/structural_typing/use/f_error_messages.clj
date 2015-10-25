(ns structural-typing.use.f-error-messages
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(fact "in type descriptions"
  (fact "bad types"
    (type! :X 1.5) => (throws "Error in a condensed type description: `1.5` is not allowed"))
  (fact "nil"
    (type! :X (:x pos?)) => (throws "One of your condensed type descriptions evaluated to `nil`"))

  (fact "old-fashioned vectors instead of `requires`"
    (type! :X [:a :b]) => (throws #"\[:a :b\] is the old-style description"))
  

  )

(future-fact "There should be an error if there's an instance of `includes` in a path")

(fact "error out if you use `requires` on the right-hand side"
  (type! :Line {(each-of :head :tail) (requires :x :y)})
  )


