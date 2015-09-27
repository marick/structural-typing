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

