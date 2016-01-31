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


(defrecord RecordVersion [a b])
(defrecord OtherRecordVersion [a b])

(fact kvs
  (let [p (subject/kvs {:a 1, :b 2})]
    (p {:a 1, :b 2}) => true
    (p {:a 2, :b 2}) => false
    (p (->RecordVersion 1 2)) => true
    (p (map->RecordVersion {:a 1, :b 2, :c 3})) => false

    (both-names p) => "(kvs {:a 1, :b 2})"

    (explain-lifted p (exval {:a 1, :b 3}))
    => (just ":x should be structurally equal to `{:a 1, :b 2}`; it is `{:a 1, :b 3}`"))

  (fact "the expected value can also be a record"
    (let [p (subject/kvs (RecordVersion. 1 2))]
      (p {:a 1 :b 2}) => true
      (p {:a 1 :b 3}) => false
      (p (OtherRecordVersion. 1 2)) => true

      (both-names p) => "(kvs {:a 1, :b 2})"

      (explain-lifted p (exval {:a 1, :b 3}))
      => (just ":x should be structurally equal to `{:a 1, :b 2}`; it is `{:a 1, :b 3}`"))))


(defrecord TwoKeys [a b])

(fact at-most-keys
  (let [p (subject/at-most-keys :a :b)]
    (map p [{:a 1} {:a 1 :b 1} {:a 1 :b 1 :c 1}]) => [true true false]
    (both-names p) => "(at-most-keys :a :b)"
    (explain-lifted p (exval {:a 1, :b 1, :c 1} [:x]))
    => (just ":x has extra keys: #{:c}; it is {:a 1, :b 1, :c 1}")

    (fact "a nil-valued key actually counts as present (as distinct from a missing key)"
      (p {:a nil :b nil, :c nil}) => false)

    (fact "also works for records"
      (p (map->TwoKeys {:a 1})) => true
      (p (map->TwoKeys {:a 1, :b 1})) => true
      (p (map->TwoKeys {:a 1, :b 1, :c 1})) => false)))


(fact exactly-keys
  (let [p (subject/exactly-keys :a :b)]
    (both-names p) => "(exactly-keys :a :b)"
    (map p [{:a 1} {:a 1 :b 1} {:a 1 :b 1 :c 1}]) => [false true false]

    (fact "extra keys in actual"
      (explain-lifted p (exval {:a 1, :b 1, :c 1} [:x]))
      => (just ":x has extra keys: #{:c}; it is {:a 1, :b 1, :c 1}"))
  
    (fact "missing keys in actual"
      (explain-lifted p (exval {:a 1} [:x]))
      => (just ":x has missing keys: #{:b}; it is {:a 1}"))

    (fact "nil keys are counted"
      (p {:a nil, :b nil}) => true
      (p {:a nil, :b nil, :c nil}) => false)

    (fact "also works for records"
      (p (map->TwoKeys {:a 1})) => true ; because of nil keys
      (p (map->TwoKeys {:a 1, :b 1})) => true
      (p (map->TwoKeys {:a 1, :b 1, :c 1})) => false)))


