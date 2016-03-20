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

(fact matches
  (let [p (subject/matches #"a+")]
    (p "a") => true
    (p "aaa") => true
    (p "ba") => true
    (p "") => false
    (p 5) => (throws)  ; but that's OK because it's lifted.

    (both-names p) => "(matches #\"a+\")"
    (explain-lifted p (exval 8 [:x])) => [":x should match #\"a+\"; it is `8`"]
    (explain-lifted p (exval "8" [:x])) => [":x should match #\"a+\"; it is \"8\""]))

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


(fact "If you expect a bigdecimal-ish number, == is accepted."
  (fact "exact equality has problems with BigDecimal"
    (let [p (subject/exactly 1M)]
      (p 1) => false   ; !
      (p 1N) => false  ; !
      (p 1M) => true))


  (fact "... that exactly== doesn't"
    (let [p (subject/exactly== 1M)]
      (p 1) => true
      (p 1N) => true
      (p 1M) => true))

  (fact "exact equality has problems with BigInt"
    (let [p (subject/exactly== 1N)]
      (= 1N 1) => true
      (= 1N 1N) => true
      (= 1N 1M) => false))   ; !

  (fact "... that exactly== doesn't"
    (let [p (subject/exactly== 1N)]
      (p 1) => true
      (p 1N) => true
      (p 1M) => true))

  (fact "inequalities still work"
    (let [p (subject/exactly== 1M)]
      (p 2) => false
      (p 2N) => false
      (p 2M) => false))

  (fact "name"
    (let [p (subject/exactly== 1M)]
      (both-names p) => "(exactly== 1M)"))

  (fact "the error message differs from normal exact comparisons"
    (check-for-explanations {:a (subject/exactly== 1M)} {:a 3})
    => (just ":a should be `==` to `1M`; it is `3`")
    (check-for-explanations {:a (subject/exactly== 1)} {:a 3M})
    => (just ":a should be `==` to `1`; it is `3M`")
    (check-for-explanations {:a (subject/exactly== 1M)} {:a 3N})
    => (just ":a should be `==` to `1M`; it is `3N`"))

  (fact "even though `==` blows up with non-numbers, we're fine"
    (check-for-explanations {:a (subject/exactly== 1)} {:a :fred})
    => (just ":a should be `==` to `1`; it is `:fred`")))


(fact "not-empty?"
  (both-names subject/not-empty?) => "not-empty?"
  (subject/not-empty? nil) => false
  (subject/not-empty? []) => false
  (subject/not-empty? [1]) => true
  (check-for-explanations subject/not-empty? 3) => ["Value should be a non-empty collection; it is `3`"])
