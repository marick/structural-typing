(ns structural-typing.api.f-predicates
  (:require [structural-typing.api.predicates :as subject]
            [structural-typing.mechanics.m-lifting-predicates :refer [lift]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))

(fact member
  (let [lifted (lift (subject/member 1 2 3))
        result (e/run-left (lifted {:leaf-value 8 :path [:x]}))]
    result => (contains {:path [:x]
                         :leaf-value 8})

    ((:error-explainer result) result) => ":x should be a member of (1 2 3); it is `8`"))
