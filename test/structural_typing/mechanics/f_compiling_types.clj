(ns structural-typing.mechanics.f-compiling-types
  (:require [com.rpl.specter :as specter])
  (:require [structural-typing.mechanics.compiling-types :as subject]
            [structural-typing.mechanics.m-preds :as pred]
            [structural-typing.api.path :as path]
            [structural-typing.api.custom :as custom]
            [structural-typing.mechanics.canonicalizing-types :refer [canonicalize]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))


(fact "compile multiple predicates into a function that checks each of them"
  (let [input {:leaf-value 1 :whole-value {:x 1} :path [:x]}
        oopsies ((subject/compile-predicates [even? odd?]) input)]
    oopsies => (just (contains (assoc input :predicate (exactly even?))))
    (custom/explanations oopsies) => [":x should be `even?`; it is `1`"])

  (let [input {:leaf-value -3 :whole-value {[:x :y] -3} :path [:x :y]}
        oopsies ((subject/compile-predicates [pos? #'even?]) input)]
    oopsies => (just (contains (assoc input :predicate (exactly pos?)))
                    (contains (assoc input :predicate (exactly #'even?))))
    ;; Note they are sorted.
    (custom/explanations oopsies) => ["[:x :y] should be `even?`; it is `-3`"
                                      "[:x :y] should be `pos?`; it is `-3`"])

  (let [input {:leaf-value -3 :whole-value {[:x :y] -3} :path [:x :y]}
        oopsies ((subject/compile-predicates [(->> pos? (pred/show-as "POS!"))]) input)]
    (custom/explanations oopsies) => ["[:x :y] should be `POS!`; it is `-3`"])

  (let [input {:leaf-value "string" :whole-value {[:x] "string"} :path [:x]}
        oopsies ((subject/compile-predicates [pos?]) input)]
    (custom/explanations oopsies) => [":x should be `pos?`; it is `\"string\"`"]))


(fact "compiling a path returns functions that select values and add to paths"
  (fact "functions without collection descriptions"
    (let [[selector leaf-selector unique-path-maker] (subject/compile-path [:a :b])
          only-expected-leaf 1]
      (specter/compiled-select selector {:a {:b 1}}) => [only-expected-leaf]
      (leaf-selector only-expected-leaf) => 1
      (unique-path-maker only-expected-leaf 0) => [:a :b]))

  (fact "with an ALL collection description"
    (let [[selector leaf-selector unique-path-maker] (subject/compile-path [:a path/ALL :b])
          only-expected-leaf [0 1]]
      (specter/compiled-select selector {:a [{:b 1}]}) => [only-expected-leaf]
      (leaf-selector only-expected-leaf) => 1
      (unique-path-maker only-expected-leaf) => [:a 0 :b]))
)
    
  

(fact "compiling a whole type"
  (fact "Simple case"
    (custom/explanations ((subject/compile-type (canonicalize {} [:a])) {}))
    => (just ":a must exist and be non-nil"))

  (fact "An optional value"
    (let [odd-if-exists (subject/compile-type (canonicalize {} {:a odd?}))]
      (custom/explanations (odd-if-exists {})) => empty?
      (custom/explanations (odd-if-exists {:a 2})) => (just ":a should be `odd?`; it is `2`")
      (custom/explanations (odd-if-exists {:a 3})) => empty?)

    (fact "Note the difference from a required value"
      (let [odd-and-exists (subject/compile-type (canonicalize {} [:a] {:a odd?}))]
        (custom/explanations (odd-and-exists {})) => (just ":a must exist and be non-nil")
        (custom/explanations (odd-and-exists {:a 2})) => (just ":a should be `odd?`; it is `2`")
        (custom/explanations (odd-and-exists {:a 3})) => empty?)))

  (fact "a path"
    (let [odd-and-exists (subject/compile-type (canonicalize {} [[:a :b]] {:a {:b odd?}}))]
      (custom/explanations (odd-and-exists {})) => (just "[:a :b] must exist and be non-nil")
      (custom/explanations (odd-and-exists {:a "hi"})) => (just "[:a :b] must exist and be non-nil")
      (custom/explanations (odd-and-exists {:a {:b 2}})) => (just "[:a :b] should be `odd?`; it is `2`")
      (custom/explanations (odd-and-exists {:a {:b 3}})) => empty?))

  (fact "a path"
    (let [odd-and-exists (subject/compile-type (canonicalize {} [[:a :b]] {:a {:b odd?}}))]
      (custom/explanations (odd-and-exists {})) => (just "[:a :b] must exist and be non-nil")
      (custom/explanations (odd-and-exists {:a "hi"})) => (just "[:a :b] must exist and be non-nil")
      (custom/explanations (odd-and-exists {:a {:b 2}})) => (just "[:a :b] should be `odd?`; it is `2`")
      (custom/explanations (odd-and-exists {:a {:b 3}})) => empty?))

  (fact "a path with multiple values (ALL)"
    (let [type (subject/compile-type (canonicalize {} [[:points path/ALL :x]]
                                                   {[:points path/ALL :x] even?}))]
      (custom/explanations (type {:points [{:x 1} {:x 2} {:x 3} {:y 1}]}))
      => (just "[:points 0 :x] should be `even?`; it is `1`"
               "[:points 2 :x] should be `even?`; it is `3`"
               "[:points 3 :x] must exist and be non-nil"
               :in-any-order)))

  
  (fact "multiple paths in the type"
    (let [type (subject/compile-type (canonicalize {} {:color string?
                                                       :point {:x integer?
                                                               :y integer?}}))]
      (custom/explanations (type {})) => empty? ; all optional
      (custom/explanations (type {:color "green"})) => empty?
      (custom/explanations (type {:color 1})) => (just ":color should be `string?`; it is `1`")
      (custom/explanations (type {:color "green"
                           :point {:x "1"
                                   :y "2"}}))
      => (just "[:point :x] should be `integer?`; it is `\"1\"`"
               "[:point :y] should be `integer?`; it is `\"2\"`"
               :in-any-order)))

  (fact "a path that can't be applied produces an error"
    (let [type (subject/compile-type (canonicalize {} [[:x path/ALL :y]]))]
      (type {:x 1}) => (just (contains {:leaf-value nil
                                        :path [:x path/ALL :y]
                                        :whole-value {:x 1}}))
      (custom/explanations (type {:x 1})) => (just "[:x ALL :y] is not a path into `{:x 1}`")
      (custom/explanations (type {:x :a})) => (just "[:x ALL :y] is not a path into `{:x :a}`")

      (fact "these are fine, though"
        (custom/explanations (type {:x [0]})) => (just "[:x 0 :y] must exist and be non-nil")
        (type {:x []}) => empty?)

      (fact "A path containing an array complains if prefix doesn't exist"
        (custom/explanations (type {})) => (just #":x must exist"))

      (fact "an unfortunate side effect of strings being collections"
        (custom/explanations (type {:x "string"}))
        => (contains "[:x 0 :y] must exist and be non-nil")))))
