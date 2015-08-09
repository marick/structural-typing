(ns structural-typing.f-preds
  (:require [structural-typing.preds :as subject]
            [structural-typing.surface.oopsie :as oopsie]
            [structural-typing.surface.mechanics :as mechanics])
  (:require [such.readable :as readable])
  (:use midje.sweet))

(facts "required-key starts out lifted"
  (subject/required-key {:leaf-value 5}) => []
  (readable/fn-string subject/required-key) => "required-key"
  (let [result (subject/required-key {:leaf-value nil
                                       :path [:x]})]
    result => (just (contains {:predicate-string "required-key"}))
    (oopsie/explanations result) => [":x must exist and be non-nil"]))

(fact member
  (fact "member produces a predicate"
    ( (subject/member [1 2 3]) 2) => true
    ( (subject/member [1 2 3]) 5) => false)

  (let [simple (subject/member [1 2 3])
        with-fn (subject/member [even? odd?])]
    (fact "a nice name"
      (readable/fn-string simple) => "(member [1 2 3])"
      (readable/fn-string (mechanics/lift simple)) => "(member [1 2 3])"

      (readable/fn-string with-fn) => "(member [even? odd?])"
      (readable/fn-string (mechanics/lift with-fn)) => "(member [even? odd?])")
      
     (fact "nice error messages"
      (oopsie/explanations ((mechanics/lift simple) {:leaf-value 8 :path [:x]}))
      => [":x should be a member of `[1 2 3]`; it is `8`"])))

(fact exactly
  (fact "produces a predicate"
    ( (subject/exactly 1) 1) => true
    ( (subject/exactly 3) 5) => false)

  (let [simple (subject/exactly 3)
        with-fn (subject/exactly even?)]

    (fact "a nice name"
      (readable/fn-string simple) => "(exactly 3)"
      (readable/fn-string (mechanics/lift simple)) => "(exactly 3)"

      (readable/fn-string with-fn) => "(exactly even?)"
      (readable/fn-string (mechanics/lift with-fn)) => "(exactly even?)")
      
    (fact "nice error messages"
      (oopsie/explanations ((mechanics/lift simple) {:leaf-value 8 :path [:x]}))
      => (just ":x should be exactly `3`; it is `8`"))))

(fact matches
  (future-fact "finish")
  (fact "default case is plain equality"
    ( (subject/matches "str") "str") => true
    ( (subject/matches "str") "FAIL") => false
    (readable/fn-string (subject/matches "foo")) => "(matches \"foo\")"
    (oopsie/explanations ((mechanics/lift (subject/matches "foo")) {:leaf-value 8 :path [:x]}))
    => (just ":x should match `\"foo\"`; it is `8`"))

  (fact "comparing a string to a regexp"
    ( (subject/matches #"str*") "strrr") => true
    ( (subject/matches #"str") "strr") => true
    ( (subject/matches #"str") "stR") => false)

  (fact "regex-to-regex comparisons compare string representations"
    ( (subject/matches #"str+") #"str+") => true
    ( (subject/matches #"str+") #"strr*") => false)
  )
