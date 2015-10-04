(ns structural-typing.guts.type-descriptions.f-elements
  (:require [structural-typing.guts.type-descriptions.elements :as subject]
            [structural-typing.guts.type-descriptions.readable :as readable])
  (:use midje.sweet))

(fact "RANGE"
  (let [r (subject/RANGE 3 5)]
    (fact "matches many"
      (subject/will-match-many? r) => true)
    (fact "readable"
      (readable/friendly [r :x]) => "[(RANGE 3 5) :x]")
    (fact "as a special case, ensure that ranges print correctly when there are N of them"
      (readable/friendly [r :x (subject/RANGE 30 50)]) => "[(RANGE 3 5) :x (RANGE 30 50)]")))
  
