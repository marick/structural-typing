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
  (subject/err:bad-range-target 5) => "RANGE could not be applied to `5`; it is not a sequential collection")

(facts "err:notpath"
  (subject/err:notpath [:a :k] 5) => "[:a :k] is not a path into `5`"
  (subject/err:notpath [:a :k] []) => "[:a :k] is not a path into `[]`"
  (subject/err:notpath [:a :k] '(1 2 3)) => "[:a :k] is not a path into `(1 2 3)`"
  (subject/err:notpath [:a :k] (map inc (range 0 3))) => "[:a :k] is not a path into `(1 2 3)`"
  (subject/err:notpath [:a :k] "foo") => "[:a :k] is not a path into `\"foo\"`"
  (subject/err:notpath [:a :k] :foo) => "[:a :k] is not a path into `:foo`"
  (subject/err:notpath [:a :k] 'foo) => "[:a :k] is not a path into `foo`")
