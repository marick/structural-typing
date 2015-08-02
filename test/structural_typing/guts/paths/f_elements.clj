(ns structural-typing.guts.paths.f-elements
  (:require [structural-typing.guts.paths.elements :as subject]
            [structural-typing.guts.paths.readable :as readable])
  (:use midje.sweet))

(facts "ALL"
  (fact "matches many"
    (subject/will-match-many? subject/ALL) => true)
  (fact "readable"
    (readable/friendly [subject/ALL]) => "[ALL]"))

(fact "RANGE"
  (let [r (subject/RANGE 3 5)]
    (fact "matches many"
      (subject/will-match-many? r) => true)
    (fact "readable"
      (readable/friendly [r :x]) => "[(RANGE 3 5) :x]")
    (fact "as a special case, ensure that ranges print correctly when there are N of them"
      (readable/friendly [r :x (subject/RANGE 30 50)]) => "[(RANGE 3 5) :x (RANGE 30 50)]")))
  
