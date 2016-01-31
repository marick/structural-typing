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


;; Note that maps have to be surrounded by either (exactly {}) or (key-values {}))
(defrecord R [r])
(defrecord R2 [r])
(fact "comparing Record objects"
  (fact "a leaf map compared to a predicate record will always fail because of type mismatch"
    (check-for-explanations {:k (->R 5)} {:k {:r 5}})
    => (just ":k should be a record; it is plain map `{:r 5}`")

    ;; Also true of a record inside a pred-list)
    (check-for-explanations {:k [integer? (->R 5)]} {:k {:r 5}})
    => (just ":k should be `integer?`; it is `{:r 5}`"
             ":k should be a record; it is plain map `{:r 5}`"))

  (fact "a leaf record compared to a same-typed predicate record uses ordinary equality"
    (built-like {:k (->R 5)} {:k (R. 5)}) => {:k (R. 5)}
    (check-for-explanations {:k (->R 5)} {:k (->R 50)})
    => (just ":k should be exactly `#R{:r 5}`; it is `#R{:r 50}`"))

  (fact "different types are reported differently"
    (check-for-explanations {:k (->R 5)} {:k (->R2 5)})
    => (just #":k should be a `R` record; it is R2.*:r 5}"))
  (future-fact "make printing of records as actual value prettier"))


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
