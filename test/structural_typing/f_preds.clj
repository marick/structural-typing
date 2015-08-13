(ns structural-typing.f-preds
  (:require [structural-typing.preds :as subject]
            [structural-typing.pred-writing.shapes.oopsie :as oopsie]
            [structural-typing.pred-writing.lifting :as lifting]
            [structural-typing.guts.shapes.pred :refer [show-as]])

  (:require [such.readable :as readable])
  (:use midje.sweet structural-typing.pred-writing.testutil))

(facts "required-key starts out lifted"
  (subject/required-key (exval 5)) => []
  (readable/fn-string subject/required-key) => "required-key"
  (let [result (subject/required-key (exval nil [:x]))]
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
      (readable/fn-string (lifting/lift simple)) => "(member [1 2 3])"

      (readable/fn-string with-fn) => "(member [even? odd?])"
      (readable/fn-string (lifting/lift with-fn)) => "(member [even? odd?])")
      
    (fact "nice error messages"
      (explain-lifted simple (exval 8 [:x]))
      => [":x should be a member of `[1 2 3]`; it is `8`"])))

(fact exactly
  (fact "produces a predicate"
    ( (subject/exactly 1) 1) => true
    ( (subject/exactly 3) 5) => false)

  (let [simple (subject/exactly 3)
        with-fn (subject/exactly even?)]

    (fact "a nice name"
      (readable/fn-string simple) => "(exactly 3)"
      (readable/fn-string (lifting/lift simple)) => "(exactly 3)"

      (readable/fn-string with-fn) => "(exactly even?)"
      (readable/fn-string (lifting/lift with-fn)) => "(exactly even?)")
      
    (fact "nice error messages"
      (explain-lifted simple (exval 8 [:x]))
      => (just ":x should be exactly `3`; it is `8`"))))

(fact matches
  (future-fact "finish")
  (fact "default case is plain equality"
    ( (subject/matches "str") "str") => true
    ( (subject/matches "str") "FAIL") => false
    (readable/fn-string (subject/matches "foo")) => "(matches \"foo\")"
    (explain-lifted (subject/matches "foo") (exval 8 [:x]))
    => (just ":x should match `\"foo\"`; it is `8`"))

  (fact "comparing a string to a regexp"
    ( (subject/matches #"str*") "strrr") => true
    ( (subject/matches #"str") "strr") => true
    ( (subject/matches #"str") "stR") => false)

  (fact "regex-to-regex comparisons compare string representations"
    ( (subject/matches #"str+") #"str+") => true
    ( (subject/matches #"str+") #"strr*") => false)
  )

(defn implies-error [actual]
  (contains {:leaf-value actual}))

(fact "implies - starts out lifted"
  (fact "nothing is OK"
    ( (subject/implies) {:leaf-value 5}) => empty?)

  (fact "a single pair will return oopsies (if any) for truthy lhs"
    (let [r (subject/implies odd? neg?)]
      (r {:leaf-value 2}) => empty?
      (r {:leaf-value -1}) => empty?
      (r {:leaf-value 1}) => (just (implies-error 1))))

  (fact "n pairs"
    (let [r (subject/implies odd? neg?
                             even? pos?)]
      (r {:leaf-value 2}) => empty?
      (r {:leaf-value -2}) => (just (implies-error -2))
      (r {:leaf-value -1}) => empty?
      (r {:leaf-value 1}) => (just (implies-error 1))))

  (fact "implies refuses nils, handles exceptions"
    (let [r (subject/implies odd? neg?)]
     (r nil) => empty?
     (r "string") => empty?))
    
  (fact "explanations"
    (explain-lifted (subject/implies odd? neg?) (exval 1 [:a]))
    => (just #":a should be `neg\?`; it is `1`")


    (explain-lifted (subject/implies odd? neg?
                                     integer? (->> #(> (count (str %)) 33)
                                                   (show-as "really big")))
                    (exval 1 [:a]))
    => (just #":a should be `neg\?`; it is `1`"
             #":a should be `really big`"
             :in-any-order)))
