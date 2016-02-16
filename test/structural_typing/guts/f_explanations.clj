(ns structural-typing.guts.f-explanations
  (:require [structural-typing.guts.explanations :as subject]
            [midje.sweet :refer :all]))


(facts "err:only"
  (subject/err:only 5) => "`5` is supposed to have exactly one element"
  (subject/err:only []) => "`[]` is supposed to have exactly one element"
  (subject/err:only '(1 2 3)) => "`(1 2 3)` is supposed to have exactly one element"
  (subject/err:only (map inc (range 0 3))) => "`(1 2 3)` is supposed to have exactly one element"
  (subject/err:only "foo") => "`\"foo\"` is supposed to have exactly one element"
  (subject/err:only :foo) => "`:foo` is supposed to have exactly one element"
  (subject/err:only 'foo) => "`foo` is supposed to have exactly one element")


(facts "err:bad-range-target"
  (subject/err:bad-range-target '[:a (RANGE 1 2)] {:a 5} 5)
  => "[:a (RANGE 1 2)] is not a path into `{:a 5}`; RANGE cannot make sense of non-collection `5`")

(facts "err:bad-all-target"
  (subject/err:bad-all-target '[:a ALL] {:a 5} 5)
  => "[:a ALL] is not a path into `{:a 5}`; ALL cannot make sense of non-collection `5`"
  (subject/err:bad-all-target '[:a ALL] {:a {:b 5}} {:b 5})
  => "[:a ALL] is not a path into `{:a {:b 5}}`; ALL cannot make sense of map `{:b 5}`")


(facts "err:nil-all"
  (subject/err:nil-all '[:a ALL] {}) => "[:a ALL] is not a path into `{}`; ALL would have to descend into a missing or nil collection")

(facts "err:notpath"
  (subject/err:notpath [:a :k] 5) => "[:a :k] is not a path into `5`"
  (subject/err:notpath [:a :k] []) => "[:a :k] is not a path into `[]`"
  (subject/err:notpath [:a :k] '(1 2 3)) => "[:a :k] is not a path into `(1 2 3)`"
  (subject/err:notpath [:a :k] (map inc (range 0 3))) => "[:a :k] is not a path into `(1 2 3)`"
  (subject/err:notpath [:a :k] "foo") => "[:a :k] is not a path into `\"foo\"`"
  (subject/err:notpath [:a :k] :foo) => "[:a :k] is not a path into `:foo`"
  (subject/err:notpath [:a :k] 'foo) => "[:a :k] is not a path into `foo`")
