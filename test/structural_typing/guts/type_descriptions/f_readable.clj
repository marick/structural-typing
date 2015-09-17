(ns structural-typing.guts.type-descriptions.f-readable
  (:require [structural-typing.guts.type-descriptions.readable :as subject])
  (:use midje.sweet
        structural-typing.assist.special-words))

(fact "friendly paths"
  (subject/friendly []) => "[]"
  (subject/friendly [:a]) => ":a"
  (subject/friendly [1]) => "[1]"
  (subject/friendly [:a :b]) => "[:a :b]"
  (subject/friendly [:a ALL :b]) => "[:a ALL :b]"
  (subject/friendly [:a even? :b]) => "[:a even? :b]"
  (subject/friendly [:a 0 :b]) => "[:a 0 :b]"

  (subject/friendly [:a (RANGE 1 3) :b]) => "[:a (RANGE 1 3) :b]"
  (subject/friendly [:a (RANGE 1 3) even? (RANGE 10 30)]) => "[:a (RANGE 1 3) even? (RANGE 10 30)]")
  

