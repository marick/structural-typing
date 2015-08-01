(ns structural-typing.guts.paths.f-elements
  (:require [structural-typing.guts.paths.elements :as subject])
  (:use midje.sweet))

(facts "ALL"
  (fact "has an offset of 0"
    (subject/offset subject/ALL) => 0)
  (fact "matches many"
    (subject/will-match-many? subject/ALL) => true))
