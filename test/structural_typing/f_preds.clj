(ns structural-typing.f-preds
  (:require [structural-typing.preds :as subject]
            [structural-typing.surface.oopsie :as oopsie]
            [structural-typing.guts.preds.lifted :refer [lift]])
  (:require [blancas.morph.monads :as e]
            [such.readable :as readable])
  (:use midje.sweet))

(fact member
  (fact "member produces a predicate"
    ( (subject/member [1 2 3]) 2) => true
    ( (subject/member [1 2 3]) 5) => false)

  (let [simple (subject/member [1 2 3])
        with-fn (subject/member [even? odd?])]
    (fact "a nice name"
      (readable/fn-string simple) => "(member [1 2 3])"
      (readable/fn-string (lift simple)) => "(member [1 2 3])"

      (readable/fn-string with-fn) => "(member [even? odd?])"
      (readable/fn-string (lift with-fn)) => "(member [even? odd?])")
      
     (fact "nice error messages"
      (oopsie/explanation (e/run-left ((lift simple) {:leaf-value 8 :path [:x]})))
      => ":x should be a member of `[1 2 3]`; it is `8`")))

(fact exactly
  (fact "produces a predicate"
    ( (subject/exactly 1) 1) => true
    ( (subject/exactly 3) 5) => false)

  (let [lifted (lift (subject/exactly 3))]
    (future-fact "a nice name"
      (readable/fn-string (subject/exactly [even? odd?])) => "(exactly [even? odd?])"
      (readable/fn-string lifted) => "(member [1 2 3])")

    (fact "nice error messages"
      (oopsie/explanation (e/run-left (lifted {:leaf-value 8 :path [:x]})))
      => ":x should be exactly `3`; it is `8`")))
