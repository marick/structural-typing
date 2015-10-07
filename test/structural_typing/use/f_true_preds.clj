(ns structural-typing.use.f-true-preds
  "Predicates available but not included in `type`"
  (:require [structural-typing.preds :as subject]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.assist.lifting :as lifting])
  (:require [such.readable :as readable])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))


(fact member
  (let [simple (subject/member [1 2 3])
        with-fn (subject/member [even? odd?])]
    (map simple [2 5]) => [true false]
    (both-names simple) => "(member [1 2 3])"
    (both-names with-fn) => "(member [even? odd?])"
    (explain-lifted simple (exval 8 [:x])) => [":x should be a member of `[1 2 3]`; it is `8`"]
    (explain-lifted with-fn (exval pos? [:x])) => [":x should be a member of `[even? odd?]`; it is `pos?`"]))

(fact exactly
  (let [simple (subject/exactly 3)
        with-fn (subject/exactly even?)]
    (map with-fn [even? odd?]) => [true false]
    (both-names with-fn) => "(exactly even?)"
    (explain-lifted simple (exval 8 [:x])) => (just ":x should be exactly `3`; it is `8`")
    (explain-lifted with-fn (exval pos? [:x])) => (just ":x should be exactly `even?`; it is `pos?`")))

(fact at-most-keys
  (let [p (subject/at-most-keys :a :b)]
    (map p [{:a 1} {:a 1 :b 1} {:a 1 :b 1 :c 1}]) => [true true false]
    (both-names p) => "(at-most-keys :a :b)"
    (explain-lifted p (exval {:a 1, :b 1, :c 1} [:x]))
    => (just ":x has extra keys: #{:c}; it is {:a 1, :b 1, :c 1}")))
  

(fact exactly-keys
  (let [p (subject/exactly-keys :a :b)]
    (both-names p) => "(exactly-keys :a :b)"
    (map p [{:a 1} {:a 1 :b 1} {:a 1 :b 1 :c 1}]) => [false true false]

    (fact "extra keys in actual"
      (explain-lifted p (exval {:a 1, :b 1, :c 1} [:x]))
      => (just ":x has extra keys: #{:c}; it is {:a 1, :b 1, :c 1}"))
  
    (fact "missing keys in actual"
      (explain-lifted p (exval {:a 1} [:x]))
      => (just ":x has missing keys: #{:b}; it is {:a 1}"))))
  

(fact matches
  (future-fact "finish")

  (fact "checks"
    (map (subject/matches "str") ["str" "FAIL"]) => [true false]

    ;; regexp
    ( (subject/matches #"str*") "strrr") => true
    ( (subject/matches #"str") "strrXXX") => true
    ( (subject/matches #"str") "stR") => false
    (fact "regex-to-regex comparisons compare string representations"
      ( (subject/matches #"str+") #"str+") => true
      ( (subject/matches #"str+") #"strr*") => false)
    )

  (fact "names"
    (readable/fn-string (subject/matches "foo")) => "(matches \"foo\")"
    )

  (fact "explanations"
    (explain-lifted (subject/matches "foo") (exval 8 [:x])) => (just ":x should match `\"foo\"`; it is `8`")
    (explain-lifted (subject/matches #"foo") (exval "fod" [:x])) => (just ":x should match `#\"foo\"`; it is `\"fod\"`")
  )
  )

