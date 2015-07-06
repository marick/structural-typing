(ns structural-typing.api.f-predicates
  (:require [structural-typing.api.predicates :as subject]
            [structural-typing.mechanics.lifting-predicates :refer [lift]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))

(fact member
  (fact "member produces a predicate"
    ( (subject/member 1 2 3) 2) => true
    ( (subject/member 1 2 3) 5) => false)

  (fact "helpful output"
    (let [lifted (lift (subject/member 1 2 3))
          result (e/run-left (lifted {:leaf-value 8 :path [:x]}))]
      result => (contains {:path [:x]
                           :leaf-value 8})
      ((:predicate-explainer result) result) => ":x should be a member of (1 2 3); it is `8`")))

(fact "show-as and explain-with"
  (let [pred (->> even?
                  (subject/show-as "name")
                  (subject/explain-with :predicate-string))]
    (pred 0) => true
    (pred 1) => false

    (let [result (e/run-left ( (lift pred) {:leaf-value 1}))]
      ((:predicate-explainer result) result) => "name")

    ( (lift pred) {:leaf-value 2}) => e/right?))

    
  
