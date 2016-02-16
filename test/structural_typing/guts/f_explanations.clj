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
  (subject/err:bad-range-target '[(RANGE 1 2)] 5) => "[(RANGE 1 2)] is not a path into `5`; RANGE may only descend into a sequential collection")

(facts "err:bad-all-target"
  (subject/err:bad-all-target '[ALL] 5) => "[ALL] is not a path into `5`; ALL must be a collection (but not a map)")

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
