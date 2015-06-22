(ns structural-typing.mechanics.fm-compile
  (:require [structural-typing.mechanics.m-compile :as subject]
            [structural-typing.mechanics.m-run :as run]
            [structural-typing.api.predicates :as pred]
            [structural-typing.api.path :as path]
            [structural-typing.mechanics.m-canonical :refer [canonicalize]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))


(fact "compile multiple predicates into a function that checks each of them"
  (let [input {:leaf-value 1 :whole-value {:x 1} :path [:x]}
        result ((subject/compile-predicates [even? odd?]) input)]
    result => (just (contains (assoc input :predicate (exactly even?))))
    (run/messages result) => [":x should be `even?`; it is `1`"])

  (let [input {:leaf-value -3 :whole-value {[:x :y] -3} :path [:x :y]}
        result ((subject/compile-predicates [pos? #'even?]) input)]
    result => (just (contains (assoc input :predicate (exactly pos?)))
                    (contains (assoc input :predicate (exactly #'even?))))
    (run/messages result) => ["[:x :y] should be `pos?`; it is `-3`"
                          "[:x :y] should be `even?`; it is `-3`"])

  (let [input {:leaf-value -3 :whole-value {[:x :y] -3} :path [:x :y]}
        result ((subject/compile-predicates [(->> pos? (pred/show-as "POS!"))]) input)]
    (run/messages result) => ["[:x :y] should be `POS!`; it is `-3`"])

  (let [input {:leaf-value "string" :whole-value {[:x] "string"} :path [:x]}
        result ((subject/compile-predicates [pos?]) input)]
    (run/messages result) => [":x should be `pos?`; it is `\"string\"`"]))


(fact "intermediate step - a function that returns all the errors for one path"
  (let [error-fn (subject/compile-predicates [odd?])
        result (subject/results-for-one-path {:xs [{:y 1} {:y 2} {:y 3}]}
                                             ["1" 2 3]
                                             [:xs path/ALL :y]
                                             error-fn)]
    result => (just (contains {:path [:xs path/ALL :y] :leaf-value "1"})
                    (contains {:path [:xs path/ALL :y] :leaf-value 2})
                    :in-any-order)))


(fact "compiling a whole type"
  (fact "Simple case"
    (run/messages ((subject/compile-type (canonicalize {} [:a])) {}))
    => (just ":a must exist and be non-nil"))

  (fact "An optional value"
    (let [odd-if-exists (subject/compile-type (canonicalize {} {:a odd?}))]
      (run/messages (odd-if-exists {})) => empty?
      (run/messages (odd-if-exists {:a 2})) => (just ":a should be `odd?`; it is `2`")
      (run/messages (odd-if-exists {:a 3})) => empty?)

    (fact "Note the difference from a required value"
      (let [odd-and-exists (subject/compile-type (canonicalize {} [:a] {:a odd?}))]
        (run/messages (odd-and-exists {})) => (just ":a must exist and be non-nil")
        (run/messages (odd-and-exists {:a 2})) => (just ":a should be `odd?`; it is `2`")
        (run/messages (odd-and-exists {:a 3})) => empty?)))

  (fact "a path"
    (let [odd-and-exists (subject/compile-type (canonicalize {} [[:a :b]] {:a {:b odd?}}))]
      (run/messages (odd-and-exists {})) => (just "[:a :b] must exist and be non-nil")
      (run/messages (odd-and-exists {:a "hi"})) => (just "[:a :b] must exist and be non-nil")
      (run/messages (odd-and-exists {:a {:b 2}})) => (just "[:a :b] should be `odd?`; it is `2`")
      (run/messages (odd-and-exists {:a {:b 3}})) => empty?))

  (fact "a path"
    (let [odd-and-exists (subject/compile-type (canonicalize {} [[:a :b]] {:a {:b odd?}}))]
      (run/messages (odd-and-exists {})) => (just "[:a :b] must exist and be non-nil")
      (run/messages (odd-and-exists {:a "hi"})) => (just "[:a :b] must exist and be non-nil")
      (run/messages (odd-and-exists {:a {:b 2}})) => (just "[:a :b] should be `odd?`; it is `2`")
      (run/messages (odd-and-exists {:a {:b 3}})) => empty?))

  (fact "a path with multiple values (ALL)"
    (let [type (subject/compile-type (canonicalize {} [[:points path/ALL :x]]
                                                   {[:points path/ALL :x] even?}))]
      (run/messages (type {:points [{:x 1} {:x 2} {:x 3} {:y 1}]}))
      => (just "[:points ALL :x] should be `even?`; it is `1`"
               "[:points ALL :x] should be `even?`; it is `3`"
               "[:points ALL :x] must exist and be non-nil"
               :in-any-order)))

  
  (fact "multiple paths in the type"
    (let [type (subject/compile-type (canonicalize {} {:color string?
                                                       :point {:x integer?
                                                               :y integer?}}))]
      (run/messages (type {})) => empty? ; all optional
      (run/messages (type {:color "green"})) => empty?
      (run/messages (type {:color 1})) => (just ":color should be `string?`; it is `1`")
      (run/messages (type {:color "green"
                           :point {:x "1"
                                   :y "2"}}))
      => (just "[:point :x] should be `integer?`; it is `\"1\"`"
               "[:point :y] should be `integer?`; it is `\"2\"`"
               :in-any-order))))

