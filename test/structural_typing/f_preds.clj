(ns structural-typing.f-preds
  (:require [structural-typing.preds :as subject]
            [structural-typing.pred-writing.shapes.oopsie :as oopsie]
            [structural-typing.pred-writing.lifting :as lifting]
            [structural-typing.guts.shapes.pred :refer [show-as]])
  (:require [such.readable :as readable])
  (:use midje.sweet structural-typing.pred-writing.testutil))


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


(fact "implies - starts out lifted"
  (fact "nothing is OK"
    ( (subject/implies) (exval 5)) => empty?)

  (fact "a single pair will return oopsies (if any) for truthy lhs"
    (let [r (subject/implies odd? neg?)]
      (r (exval 2)) => empty?
      (r (exval -1)) => empty?
      (r (exval 1)) => (just (oopsie-for 1))))

  (fact "n pairs"
    (let [r (subject/implies odd? neg?
                             even? pos?)]
      (r (exval 2)) => empty?
      (r (exval -2)) => (just (oopsie-for -2))
      (r (exval -1)) => empty?
      (r (exval 1)) => (just (oopsie-for 1))))

  (fact "implies refuses nils, handles exceptions"
    (let [r (subject/implies odd? neg?)]
     (r nil) => empty?
     (r "string") => empty?))
    
  (fact "explanations"
    (explain-lifted (subject/implies odd? neg?) (exval 1 [:a]))
    => (just #":a should be `neg\?`; it is `1`")

    (explain-lifted (subject/implies odd? neg?
                                     integer? (->> #(> (count (str %)) 33)
                                                   (show-as "really big")))
                    (exval 1 [:a]))
    => (just #":a should be `neg\?`; it is `1`"
             #":a should be `really big`"
             :in-any-order)))
