(ns structural-typing.use.condensed-type-descriptions.f-what-counts-as-a-predicate
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)

(fact "Here are legitimate predicates"
  (check-for-explanations {:k odd?} {:k 2}) => [(err:shouldbe :k "odd?" 2)]
  (check-for-explanations {:k (pred/exactly 5)} {:k 1}) => [(err:shouldbe :k "exactly `5`" 1 :omit-quotes)])

(future-fact "Use midje-equality instead of `exactly` for coercion")

(fact "Here is a non-predicate, which is coerced to `exactly`"
  (built-like {:k "expected"} {:k "expected"}) => {:k "expected"}
  (check-for-explanations {:k 5} {:k 1}) => [(err:shouldbe :k "exactly `5`" 1 :omit-quotes)])

(facts "about interactions between coercions and shorthand"
  (fact "coercion applies for maps on the right-hand side of a type expression"
    (let [input {:k {:j 5}}]
      (built-like {[:k] {:j odd?}} input) => input
      (built-like {[:k] {:j 5}} input) => input
      (check-for-explanations {[:k] {:j 6}} input)
      => [(err:shouldbe [:k :j] "exactly `6`" 5 :omit-quotes)]))
  )



(start-over!)
