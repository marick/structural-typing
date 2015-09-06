(ns structural-typing.use.f-preds
  "Predicates available but not included in `type`"
  (:require [structural-typing.preds :as subject]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.assist.lifting :as lifting]
            [structural-typing.assist.annotating :refer [show-as]]
            [structural-typing.guts.preds.core :refer [required-key]]
            [structural-typing.guts.type-descriptions.substituting :refer [includes]])
  (:require [such.readable :as readable])
  (:use midje.sweet structural-typing.assist.testutil))


;;; Easy preds

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

;;; Predicates that start out lifted

(fact "utility for implies: all-of"
  (:type-descriptions (subject/force-all-of odd?)) => [odd?]
  (:type-descriptions (subject/force-all-of (subject/all-of odd? even?))) => [odd? even?])
  

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
    (let [r (subject/implies #(even? (:a %)) [:c :b])]
      (run r {}) => empty?
      (run r {:a 1}) => empty?
      (oopsie/explanations (run r {:a 2}))
      => (just "[:x :b] must exist and be non-nil"
               "[:x :c] must exist and be non-nil")))

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
        (oopsie/explanations (run r {:a 1})) => (just "[:x :b] must exist and be non-nil")))

    (fact "expanding a condensed type description"
      (let [r (subject/implies :a [:b :c :d])]
        (run r {:a 1, :b 2, :c 3, :d 4}) => empty?
        (oopsie/explanations (run r {:a 1, :b 2, :d 4}))
        => (just "[:x :c] must exist and be non-nil")))

    (fact "including preexisting type definitions"
      (let [r (subject/implies (comp even? :a) {:b [required-key (includes :Point)]})
            type-map {:Point {:x integer? :y integer?}}]
        (run r {} type-map) => empty?
        (run r {:a 1 :b 1} type-map) => empty?
        (run r {:a 2 :b {:x 1 :y 1}} type-map) => empty?
        (oopsie/explanations (run r {:a 2 :b {:x 1 :y 1.0}} type-map))
        => (just #"\[:x :b :y\] should be `integer\?`; it is `1.0`"))))))
