(ns structural-typing.f-preds
  (:require [structural-typing.preds :as subject]
            [structural-typing.api.custom :as custom]
            [structural-typing.mechanics.lifting-predicates :refer [lift]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))

(fact member
  (fact "member produces a predicate"
    ( (subject/member [1 2 3]) 2) => true
    ( (subject/member [1 2 3]) 5) => false)

  (let [lifted (lift (subject/member [1 2 3]))]
    (future-fact "a nice name"
      (custom/friendly-function-name (subject/member [1 2 3])) => "(member [1 2 3])"
      (custom/friendly-function-name (subject/member [even? odd?])) => "(member [even? odd?])"
      (custom/friendly-function-name lifted) => "(member [1 2 3])")

    (fact "nice error messages"
      (custom/explanation (e/run-left (lifted {:leaf-value 8 :path [:x]})))
      => ":x should be a member of `[1 2 3]`; it is `8`")))

(fact exactly
  (fact "produces a predicate"
    ( (subject/exactly 1) 1) => true
    ( (subject/exactly 3) 5) => false)

  (let [lifted (lift (subject/exactly 3))]
    (future-fact "a nice name"
      (custom/friendly-function-name (subject/exactly [even? odd?])) => "(exactly [even? odd?])"
      (custom/friendly-function-name lifted) => "(member [1 2 3])")

    (fact "nice error messages"
      (custom/explanation (e/run-left (lifted {:leaf-value 8 :path [:x]})))
      => ":x should be exactly `3`; it is `8`")))
