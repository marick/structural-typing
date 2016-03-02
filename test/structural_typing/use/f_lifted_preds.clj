(ns structural-typing.use.f-lifted-preds
  "Predicates that are special because they start out lifted"
  (:require [structural-typing.preds :as subject]
            [structural-typing.assist.oopsie :as oopsie])
  (:require [such.readable :as readable])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))

(fact "implies is a three level predicate"
  ;; implies -> fn that replaces `includes` -> lifted fn
  (letfn [(run
            ([compiled leaf-value type-map]
               ((compiled type-map) (exval leaf-value [:x])))
            ([compiled leaf-value]
               (run compiled leaf-value {})))]
               
    (fact "Can be an empty arglist"
      (run (subject/implies) 5) => empty?)

    (fact "a single pair will return oopsies (if any) for truthy lhs"
      (let [r (subject/implies odd? neg?)]
      (run r 2) => empty?
      (run r -1) => empty?
      (run r 1) => (just (oopsie-for 1))))

  (fact "n pairs"
    (let [r (subject/implies odd? neg?
                             even? pos?)]
      (run r 2) => empty?
      (run r -2) => (just (oopsie-for -2))
      (run r -1) => empty?
      (run r 1) => (just (oopsie-for 1))))

  (fact "implies refuses nils, handles exceptions"
    (let [r (subject/implies odd? neg?)]
      (run r nil) => empty?
      (run r "string") => empty?))
    
  (fact "explanations"
    (oopsie/explanations (run (subject/implies odd? neg?) 1))
    => (just #":x should be `neg\?`; it is `1`")

    (oopsie/explanations (run (subject/implies odd? neg?
                                               integer? (->> #(> (count (str %)) 33)
                                                             (show-as "really big")))
                              1))
    => (just #":x should be `neg\?`; it is `1`"
             #":x should be `really big`"))

  (fact "use with substructures"
    (let [r (subject/implies #(even? (:a %)) (requires :c :b))]
      (run r {}) => empty?
      (run r {:a 1}) => empty?
      (oopsie/explanations (run r {:a 2}))
      => (just (err:missing [:x :b])
               (err:missing [:x :c]))))

  (fact one-of
    (let [bigger #(> (count (str %)) 3)
          smaller #(< (count (str %)) 6)
          r (subject/implies odd? (subject/all-of bigger smaller))]
      (oopsie/explanations (run r 111)) => (just ":x should be `bigger`; it is `111`")
      (run r 1111) => empty?
      (run r 11111) => empty?
      (oopsie/explanations (run r 111111)) =>  (just ":x should be `smaller`; it is `111111`")
      (run r 2) => empty?))

  (fact "examples used in the documentation"
    (fact "possibly the most common case"
      (let [r (subject/implies :a :b)]
        (run r {}) => empty?
        (run r {:b 1}) => empty?
        (run r {:a 1, :b 2}) => empty?
        (oopsie/explanations (run r {:a 1})) => (just "[:x :b] does not exist")))

    (fact "expanding a condensed type description"
      (let [r (subject/implies :a (requires :b :c :d))]
        (run r {:a 1, :b 2, :c 3, :d 4}) => empty?
        (oopsie/explanations (run r {:a 1, :b 2, :d 4}))
        => (just (err:missing [:x :c]))))

    (fact "including preexisting type definitions"
      (let [r (subject/implies (comp even? :a) {:b [required-path (includes :Point)]})
            type-map {:Point {:x integer? :y integer?}}]
        (run r {} type-map) => empty?
        (run r {:a 1 :b 1} type-map) => empty?
        (run r {:a 2 :b {:x 1 :y 1}} type-map) => empty?
        (oopsie/explanations (run r {:a 2 :b {:x 1 :y 1.0}} type-map))
        => (just #"\[:x :b :y\] should be `integer\?`; it is `1.0`"))))))
