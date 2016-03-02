(ns structural-typing.assist.f-oopsie
  (:require [structural-typing.assist.oopsie :as subject])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))

(fact "friendly-path"
  (subject/friendly-path {:path [:a]}) => ":a"
  (subject/friendly-path {:path [:a :b]}) => "[:a :b]"
  (subject/friendly-path {:path [:a ALL :b]}) => "[:a ALL :b]"
  (subject/friendly-path {:path []}) => "Value")

(fact "explanations sorts and removes different"
  (prerequisites
   (subject/explanation 1) => "last"
   (subject/explanation 2) => "duplicate"
   (subject/explanation 3) => "duplicate"
   (subject/explanation 4) => "am first")

  (subject/explanations [1 2 3 4]) => ["am first" "duplicate" "last"])
