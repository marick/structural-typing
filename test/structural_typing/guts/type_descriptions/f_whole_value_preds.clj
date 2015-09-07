(ns structural-typing.guts.type-descriptions.f-whole-value-preds
  (:require [structural-typing.guts.type-descriptions.whole-value-preds :as subject])
  (:use midje.sweet))

(fact dc:preds->maps
  (subject/dc:preds->maps [even?]) => (just {[] [even?]})
  (subject/dc:preds->maps [[:a [:b]] {:a odd?} even?])
  =>                      [[:a [:b]] {:a odd?} {[] [even?]}]) 

