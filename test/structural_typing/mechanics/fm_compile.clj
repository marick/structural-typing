(ns structural-typing.mechanics.fm-compile
  (:require [structural-typing.mechanics.m-compile :as subject]
            [structural-typing.mechanics.m-run :as run]
            [structural-typing.api.predicates :as pred]
            [structural-typing.api.path :as path]
            [structural-typing.api.defaults :as default]
            [structural-typing.mechanics.m-canonical :refer [canonicalize]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))


(fact "compile multiple predicates into a function that checks each of them"
  (let [input {:leaf-value 1 :whole-value {:x 1} :path [:x]}
        oopsies ((subject/compile-predicates [even? odd?]) input)]
    oopsies => (just (contains (assoc input :predicate (exactly even?))))
    (default/explanations oopsies) => [":x should be `even?`; it is `1`"])

  (let [input {:leaf-value -3 :whole-value {[:x :y] -3} :path [:x :y]}
        oopsies ((subject/compile-predicates [pos? #'even?]) input)]
    oopsies => (just (contains (assoc input :predicate (exactly pos?)))
                    (contains (assoc input :predicate (exactly #'even?))))
    (default/explanations oopsies) => ["[:x :y] should be `pos?`; it is `-3`"
                          "[:x :y] should be `even?`; it is `-3`"])

  (let [input {:leaf-value -3 :whole-value {[:x :y] -3} :path [:x :y]}
        oopsies ((subject/compile-predicates [(->> pos? (pred/show-as "POS!"))]) input)]
    (default/explanations oopsies) => ["[:x :y] should be `POS!`; it is `-3`"])

  (let [input {:leaf-value "string" :whole-value {[:x] "string"} :path [:x]}
        oopsies ((subject/compile-predicates [pos?]) input)]
    (default/explanations oopsies) => [":x should be `pos?`; it is `\"string\"`"]))


(fact "intermediate step - a function that returns all the errors for one path"
  (let [error-fn (subject/compile-predicates [odd?])
        oopsies (subject/oopsies-for-one-path {:xs [{:y 1} {:y 2} {:y 3}]}
                                             ["1" 2 3]
                                             [:xs path/ALL :y]
                                             error-fn)]
    oopsies => (just (contains {:path [:xs path/ALL :y] :leaf-value "1" :leaf-index 0 :leaf-count 3})
                    (contains {:path [:xs path/ALL :y] :leaf-value 2 :leaf-index 1 :leaf-count 3})
                    :in-any-order)))


(fact "compiling a whole type"
  (fact "Simple case"
    (default/explanations ((subject/compile-type (canonicalize {} [:a])) {}))
    => (just ":a must exist and be non-nil"))

  (fact "An optional value"
    (let [odd-if-exists (subject/compile-type (canonicalize {} {:a odd?}))]
      (default/explanations (odd-if-exists {})) => empty?
      (default/explanations (odd-if-exists {:a 2})) => (just ":a should be `odd?`; it is `2`")
      (default/explanations (odd-if-exists {:a 3})) => empty?)

    (fact "Note the difference from a required value"
      (let [odd-and-exists (subject/compile-type (canonicalize {} [:a] {:a odd?}))]
        (default/explanations (odd-and-exists {})) => (just ":a must exist and be non-nil")
        (default/explanations (odd-and-exists {:a 2})) => (just ":a should be `odd?`; it is `2`")
        (default/explanations (odd-and-exists {:a 3})) => empty?)))

  (fact "a path"
    (let [odd-and-exists (subject/compile-type (canonicalize {} [[:a :b]] {:a {:b odd?}}))]
      (default/explanations (odd-and-exists {})) => (just "[:a :b] must exist and be non-nil")
      (default/explanations (odd-and-exists {:a "hi"})) => (just "[:a :b] must exist and be non-nil")
      (default/explanations (odd-and-exists {:a {:b 2}})) => (just "[:a :b] should be `odd?`; it is `2`")
      (default/explanations (odd-and-exists {:a {:b 3}})) => empty?))

  (fact "a path"
    (let [odd-and-exists (subject/compile-type (canonicalize {} [[:a :b]] {:a {:b odd?}}))]
      (default/explanations (odd-and-exists {})) => (just "[:a :b] must exist and be non-nil")
      (default/explanations (odd-and-exists {:a "hi"})) => (just "[:a :b] must exist and be non-nil")
      (default/explanations (odd-and-exists {:a {:b 2}})) => (just "[:a :b] should be `odd?`; it is `2`")
      (default/explanations (odd-and-exists {:a {:b 3}})) => empty?))

  (fact "a path with multiple values (ALL)"
    (let [type (subject/compile-type (canonicalize {} [[:points path/ALL :x]]
                                                   {[:points path/ALL :x] even?}))]
      (default/explanations (type {:points [{:x 1} {:x 2} {:x 3} {:y 1}]}))
      => (just "[:points ALL :x][0] should be `even?`; it is `1`"
               "[:points ALL :x][2] should be `even?`; it is `3`"
               "[:points ALL :x][3] must exist and be non-nil"
               :in-any-order)))

  
  (fact "multiple paths in the type"
    (let [type (subject/compile-type (canonicalize {} {:color string?
                                                       :point {:x integer?
                                                               :y integer?}}))]
      (default/explanations (type {})) => empty? ; all optional
      (default/explanations (type {:color "green"})) => empty?
      (default/explanations (type {:color 1})) => (just ":color should be `string?`; it is `1`")
      (default/explanations (type {:color "green"
                           :point {:x "1"
                                   :y "2"}}))
      => (just "[:point :x] should be `integer?`; it is `\"1\"`"
               "[:point :y] should be `integer?`; it is `\"2\"`"
               :in-any-order))))

