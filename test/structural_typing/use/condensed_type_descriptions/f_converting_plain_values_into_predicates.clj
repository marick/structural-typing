(ns structural-typing.use.condensed-type-descriptions.f-converting-plain-values-into-predicates
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

(fact "Here is a non-predicate, which is coerced to `exactly`"
  (built-like {:k "expected"} {:k "expected"}) => {:k "expected"}
  (check-for-explanations {:k 5} {:k 1}) => [(err:shouldbe :k "exactly `5`" 1 :omit-quotes)])

(fact "regular expressions"
  (fact "are compared to string with `re-find`"
    (built-like {:k #"a+b"} {:k "aaaab"}) => {:k "aaaab"}
    (check-for-explanations {:k #"a+b"} {:k "b"}) => (just ":k should match #\"a+b\"; it is \"b\""))
  (fact "comparing two regular expressions uses string equality"
    (str (built-like {:k #"a+b"} {:k #"a+b"})) => (str {:k #"a+b"})
    (check-for-explanations {:k #"a+b"} {:k #"aa*b"}) => (just ":k should match #\"a+b\"; it is #\"aa*b\""))

  (fact "bogus values"
    (check-for-explanations {:k #"a+b"} {:k 5}) => (just ":k should match #\"a+b\"; it is `5`")))


(future-fact "comparing key-value objects"
  (fact "a leaf map compared to a predicate map works by equality")
  (fact "a leaf record compared to a predicate map ignores the record type")
  (fact "a leaf map compared to a predicate record will always fail because of type mismatch")
  (fact "a leaf record compared to a predicate record uses ordinary equality"))

(future-fact "BigDecimals do not fail when compared to integers and the like")

(facts "about interactions between coercions and shorthand"
  (fact "coercion applies for maps on the right-hand side of a type expression"
    (let [input {:k {:j 5}}]
      (built-like {[:k] {:j odd?}} input) => input
      (built-like {[:k] {:j 5}} input) => input
      (check-for-explanations {[:k] {:j 6}} input)
      => [(err:shouldbe [:k :j] "exactly `6`" 5 :omit-quotes)]))
  )

(start-over!)
