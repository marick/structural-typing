(ns structural-typing.use.condensed-type-descriptions.f-implies-consequent
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil)
  (:require [structural-typing.preds :as pred]))

(start-over!)

(fact "the antecedent can be a keyword"
  (let [type (pred/implies :a :b)]
    (built-like? type {:a 1}) => false
    (built-like? type {:a 1, :b 2}) => true
    (built-like? type {}) => true))  ; inapplicable

(fact "the antecedent can be a predicate"
  (let [type (pred/implies even? pos?)]
    (built-like? type -2) => false
    (built-like? type 2) => true
    (built-like? type -3) => true)) ; inapplicable

(fact "the antecedent can be a map"
  (let [type (pred/implies {:a even?} {:b pos?})]
    (built-like? type {:a 2, :b -2}) => false
    (built-like? type {:a 2, :b 2}) => true
    (built-like? type {:a 3, :b -1}) => true)) ; inapplicable

(fact "what the heck: requires"
  (let [type (pred/implies (requires :a [:b :c]) {:d odd?})]
    (built-like? type {:a 2, :b {:c 1} :d 2}) => false
    (built-like? type {:a 2, :b {:c 1} :d 1}) => true
    ; inapplicable cases
    (built-like? type {:a 2, :d 2}) => true
    (built-like? type {:a 2, :b 2 :d 2}) => true
    (built-like? type {:a 2, :b {} :d 2}) => true))

(fact "the antecedent can be a predicate"
  (let [type (pred/implies {:a nil?} :b)]
    (built-like? type {}) => false
    (built-like? type {:b 3}) => true
    (built-like? type {:a 1}) => true)) ; inapplicable

(fact "the antecedent can be compound"
  (let [type (pred/implies (pred/all-of (requires :a) {:a pos?}) {:b odd?})]
    (built-like? type {:a 2, :b 2}) => false
    (built-like? type {:a 2, :b 1}) => true
    ; inapplicable cases
    (built-like? type {:b 2}) => true
    (built-like? type {:a -1, :b 2}) => true))

(fact "types can be included"
  (type! :Point (requires :x :y))
  (let [type (pred/implies (includes :Point) {:b odd?})]
    (built-like? type {:x 1, :y 1, :b 2}) => false
    (built-like? type {:x 1, :y 1, :b 1}) => true
    (built-like? type {:x 1, :b 2}) => true))
  

(start-over!)
