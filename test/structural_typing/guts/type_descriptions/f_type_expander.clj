(ns structural-typing.guts.type-descriptions.f-type-expander
  (:require [structural-typing.guts.type-descriptions.type-expander :as subject])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))


(fact includes
  (let [point {[:x] [integer?] [:y] [integer?]}
        type-map {:Point point}]
    (subject/expand-throughout type-map (includes :Point)) => point
    (subject/expand-throughout type-map [(includes :Point)]) => [point]
    (subject/expand-throughout type-map {:a (includes :Point)}) => {:a point}
    (subject/expand-throughout type-map {[:a :points ALL] [required-path (includes :Point)]})
    => {[:a :points ALL] [required-path point]}
    (subject/expand-throughout type-map {:a [required-path pos?]}) => {:a [required-path pos?]}))
