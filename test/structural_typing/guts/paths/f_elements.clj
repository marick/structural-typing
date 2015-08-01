(ns structural-typing.guts.paths.f-elements
  (:require [structural-typing.guts.paths.elements :as subject]
            [structural-typing.guts.paths.readable :as readable])
  (:use midje.sweet))

(facts "ALL"
  (fact "has an offset of 0"
    (subject/offset subject/ALL) => 0)
  (fact "matches many"
    (subject/will-match-many? subject/ALL) => true)
  (fact "readable"
    (readable/friendly [subject/ALL]) => "[ALL]"))

(fact "RANGE"
  (let [r (subject/RANGE 3 5)]
    (fact "has a given offset"
      (subject/offset r) => 3)
    (fact "matches many"
      (subject/will-match-many? r) => true)
    (fact "readable"
      (readable/friendly [r :x]) => "[(RANGE 3 5) :x]")))
  
