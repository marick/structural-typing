(ns structural-typing.guts.type-descriptions.fdc-type-maps
  (:require [structural-typing.guts.type-descriptions.dc-type-maps :as subject])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))


(fact includes
  (let [point {[:x] [integer?] [:y] [integer?]}
        type-map {:Point point}]
    (subject/substitute type-map (includes :Point)) => point
    (subject/substitute type-map [(includes :Point)]) => [point]
    (subject/substitute type-map {:a (includes :Point)}) => {:a point}
    (subject/substitute type-map {[:a :points ALL] [required-key (includes :Point)]})
    => {[:a :points ALL] [required-key point]}
    (subject/substitute type-map {:a [required-key pos?]}) => {:a [required-key pos?]}))
