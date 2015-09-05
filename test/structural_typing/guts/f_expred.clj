(ns structural-typing.guts.f-expred
  (:require [structural-typing.guts.expred :as subject])
  (:use midje.sweet))


(fact "friendly-path"
  (subject/friendly-path {:path [:a]}) => ":a"
  (subject/friendly-path {:path [:a :b]}) => "[:a :b]"
  (subject/friendly-path {:path []}) => "Value")
