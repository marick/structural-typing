(ns structural-typing.use.condensed-type-descriptions.f-requires
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(fact "an empty `requires`"
  (type! :X (requires))
  (checked :X 3) => 3)
  
  
