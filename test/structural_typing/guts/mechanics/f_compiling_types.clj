(ns structural-typing.guts.mechanics.f-compiling-types
  (:require [com.rpl.specter :as specter])
  (:require [structural-typing.guts.mechanics.compiling-types :as subject]
            [structural-typing.guts.preds.annotated :refer [show-as]]
            [structural-typing.surface.oopsie :as oopsie]
            [structural-typing.guts.mechanics.canonicalizing-types :refer [canonicalize]]
            [structural-typing.guts.paths.elements :refer [ALL RANGE]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))


(fact "compile multiple predicates into a function that checks each of them"
  (let [input {:leaf-value 1 :whole-value {:x 1} :path [:x]}
        oopsies ((subject/compile-predicates [even? odd?]) input)]
    oopsies => (just (contains (assoc input :predicate (exactly even?))))
    (oopsie/explanations oopsies) => [":x should be `even?`; it is `1`"])

  (let [input {:leaf-value -3 :whole-value {[:x :y] -3} :path [:x :y]}
        oopsies ((subject/compile-predicates [pos? #'even?]) input)]
    oopsies => (just (contains (assoc input :predicate (exactly pos?)))
                    (contains (assoc input :predicate (exactly #'even?))))
    ;; Note they are sorted.
    (oopsie/explanations oopsies) => ["[:x :y] should be `even?`; it is `-3`"
                                      "[:x :y] should be `pos?`; it is `-3`"])

  (let [input {:leaf-value -3 :whole-value {[:x :y] -3} :path [:x :y]}
        oopsies ((subject/compile-predicates [(->> pos? (show-as "POS!"))]) input)]
    (oopsie/explanations oopsies) => ["[:x :y] should be `POS!`; it is `-3`"])

  (let [input {:leaf-value "string" :whole-value {[:x] "string"} :path [:x]}
        oopsies ((subject/compile-predicates [pos?]) input)]
    (oopsie/explanations oopsies) => [":x should be `pos?`; it is `\"string\"`"]))


(fact "handling ALL"
  (let [type-checker (subject/compile-type (canonicalize {} [[:points ALL :x]]
                                                         {[:points ALL :x] even?}))]
    (oopsie/explanations (type-checker {:points [{:x 1} {:x 2} {:x 3} {:y 1}]}))
    => (just "[:points 0 :x] should be `even?`; it is `1`"
             "[:points 2 :x] should be `even?`; it is `3`"
             "[:points 3 :x] must exist and be non-nil"
             :in-any-order)))

  (fact "a path with multiple values (RANGE)"
    (fact "simple case"
      (let [type-checker (subject/compile-type (canonicalize {} 
                                                             {[(RANGE 2 4)] even?}))]
        (fact "a range avoids broken values"
          (oopsie/explanations (type-checker [:wrong :wrong 2 4 :wrong])) => empty?)
        
        (fact "... but allows processing of values within the range"
          (oopsie/explanations (type-checker [:wrong :wrong 0 41 :wrong]))
          => (just "[3] should be `even?`; it is `41`"))))
    
    (fact "multiple ranges"
      (let [type-checker (subject/compile-type (canonicalize {} 
                                                             {[(RANGE 2 4) (RANGE 1 2)] even?}))]
        (fact "a range avoids broken values"
          (oopsie/explanations (type-checker [:wrong :wrong [:wrong 2 :wrong] [:wrong 4] :wrong])) => empty?)
        
        (fact "... but allows processing of values within the range"
          (oopsie/explanations (type-checker [:wrong :wrong [:wrong 1 :wrong] [:wrong 3]]))
          => (just "[2 1] should be `even?`; it is `1`"
                   "[3 1] should be `even?`; it is `3`"))))
    
    (fact "combination of RANGE and ALL"
      (let [type-checker (subject/compile-type (canonicalize {} 
                                                             {[(RANGE 2 4) ALL] even?}))]
        (fact "a range avoids broken values"
          (oopsie/explanations (type-checker [:wrong :wrong [2 4 6] [4] :wrong])) => empty?)
        
        (fact "... but allows processing of values within the range"
          (oopsie/explanations (type-checker [:wrong :wrong [0 1 2] [2 3]]))
          => (just "[2 1] should be `even?`; it is `1`"
                   "[3 1] should be `even?`; it is `3`"))))
    
    (fact "including other path elements"
      (let [type-checker (subject/compile-type
                          (canonicalize {} 
                                        {[:a (RANGE 1 3) :b (RANGE 1 5) pos?] even?}))]
        (fact "a range avoids broken values"
          (oopsie/explanations (type-checker {:a [:wrong 
                                                  {:b [1  2  2  2  2 1]}
                                                  {:b [1 -1 -1 -1 -1 1]}
                                                  :wrong]})) => empty?)
        
        (fact "... but allows processing of values within the range"
          (oopsie/explanations (type-checker {:a [:wrong
                                                  {:b [1  2  2  2  3 1]}
                                                  {:b [1 -1 -1  5 -1 1]}
                                                  :wrong]}))
          => (just "[:a 1 :b 4 pos?] should be `even?`; it is `3`"
                   "[:a 2 :b 3 pos?] should be `even?`; it is `5`")))))


(fact "compiling a whole type"
  (fact "Simple case"
    (oopsie/explanations ((subject/compile-type (canonicalize {} [:a])) {}))
    => (just ":a must exist and be non-nil"))

  (fact "An optional value"
    (let [odd-if-exists (subject/compile-type (canonicalize {} {:a odd?}))]
      (oopsie/explanations (odd-if-exists {})) => empty?
      (oopsie/explanations (odd-if-exists {:a 2})) => (just ":a should be `odd?`; it is `2`")
      (oopsie/explanations (odd-if-exists {:a 3})) => empty?)

    (fact "Note the difference from a required value"
      (let [odd-and-exists (subject/compile-type (canonicalize {} [:a] {:a odd?}))]
        (oopsie/explanations (odd-and-exists {})) => (just ":a must exist and be non-nil")
        (oopsie/explanations (odd-and-exists {:a 2})) => (just ":a should be `odd?`; it is `2`")
        (oopsie/explanations (odd-and-exists {:a 3})) => empty?)))

  (fact "a path"
    (let [odd-and-exists (subject/compile-type (canonicalize {} [[:a :b]] {:a {:b odd?}}))]
      (oopsie/explanations (odd-and-exists {})) => (just "[:a :b] must exist and be non-nil")
      (oopsie/explanations (odd-and-exists {:a "hi"})) => (just "[:a :b] must exist and be non-nil")
      (oopsie/explanations (odd-and-exists {:a {:b 2}})) => (just "[:a :b] should be `odd?`; it is `2`")
      (oopsie/explanations (odd-and-exists {:a {:b 3}})) => empty?))
  
  (fact "multiple paths in the type"
    (let [type-checker (subject/compile-type (canonicalize {} {:color string?
                                                       :point {:x integer?
                                                               :y integer?}}))]
      (oopsie/explanations (type-checker {})) => empty? ; all optional
      (oopsie/explanations (type-checker {:color "green"})) => empty?
      (oopsie/explanations (type-checker {:color 1})) => (just ":color should be `string?`; it is `1`")
      (oopsie/explanations (type-checker {:color "green"
                           :point {:x "1"
                                   :y "2"}}))
      => (just "[:point :x] should be `integer?`; it is `\"1\"`"
               "[:point :y] should be `integer?`; it is `\"2\"`"
               :in-any-order))))




(fact "a path that can't be applied produces an error"
  (fact "ALL"
    (let [type-checker (subject/compile-type (canonicalize {} [[:x ALL :y]]))]
      (type-checker {:x 1}) => (just (contains {:leaf-value nil
                                                :path [:x ALL :y]
                                                :whole-value {:x 1}}))
      (oopsie/explanations (type-checker {:x 1})) => (just "[:x ALL :y] is not a path into `{:x 1}`")
      (oopsie/explanations (type-checker {:x :a})) => (just "[:x ALL :y] is not a path into `{:x :a}`")
      
      (fact "these are fine, though"
        (oopsie/explanations (type-checker {:x [0]})) => (just "[:x 0 :y] must exist and be non-nil")
        (type-checker {:x []}) => empty?)
      
      (fact "A path containing an array complains if prefix doesn't exist"
        (oopsie/explanations (type-checker {})) => (just #":x must exist"))
      
      (fact "an unfortunate side effect of strings being collections"
        (oopsie/explanations (type-checker {:x "string"}))
        => (contains "[:x 0 :y] must exist and be non-nil"))))

  (fact "RANGE"
    (fact "ending index just fits"
      (let [type-checker (subject/compile-type (canonicalize {}
                                                             {[(RANGE 1 3)] even?}))]
        (oopsie/explanations (type-checker [0 2 4])) => empty?))

    (fact "ending index comes too soon"
      (let [type-checker (subject/compile-type (canonicalize {}
                                                             {[(RANGE 1 3)] even?}))]

        (oopsie/explanations (type-checker [0 2]))
        => (just "[(RANGE 1 3)] is not a path into `[0 2]`")))

    (fact "starting index comes too soon"
      (let [type-checker (subject/compile-type (canonicalize {}
                                                             {[(RANGE 2 5)] even?}))]

        (oopsie/explanations (type-checker [0 2]))
        => (just "[(RANGE 2 5)] is not a path into `[0 2]`")))

    (fact "in a previous bug, multiple range expressions all printed with same value"
      (let [type-checker (subject/compile-type
                          (canonicalize {} 
                                        {[:a (RANGE 1 4) :b (RANGE 1 5) pos?] even?}))]
        (oopsie/explanations (type-checker {:a [:wrong :wrong
                                                {:b [1  2  2  2  2 1]}
                                                {:b [1 -1 -1 -1 -1 1]}
                                                :wrong]}))
        => (just #"\[:a \(RANGE 1 4\) :b \(RANGE 1 5\) pos\?\] is not a path")))))

    
