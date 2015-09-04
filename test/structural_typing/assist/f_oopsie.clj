(ns structural-typing.assist.f-oopsie
  (:require [structural-typing.assist.oopsie :as subject])
  (:use midje.sweet))


(fact "friendly-path"
  (subject/friendly-path {:path [:a]}) => ":a"
  (subject/friendly-path {:path [:a :b]}) => "[:a :b]"
  (subject/friendly-path {:path []}) => "Value")
