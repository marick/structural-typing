(ns structural-typing.docs.f-wiki-what-can-be-in-a-path
 (:require [structural-typing.type :refer :all]
           [structural-typing.global-type :refer :all]
           [structural-typing.api.custom :as custom]
           [clojure.string :as str])
 (:use [midje.sweet :exclude [exactly]]))

(future-fact "shouldn't have to exclude exactly")

(start-over!)

(fact "keywords"
  (type! :X {[:a :b] integer?})
  (described-by? :X {:a {:b 1}}) => true)

(fact "ALL"
  (type! :IntArray {[ALL] integer?})
  (described-by? :IntArray [1 2 3 4]) => true
  (let [result (with-out-str (checked :IntArray [1 :a 2 :b]))]
    result => #"\[1\] should be `integer"
    result => #"\[3\] should be `integer")

  (type! :D2 {[ALL ALL] integer?})
  (let [result (with-out-str (checked :D2 [  [0 :elt-0-1] [:elt-1-0] [] [0 0 :elt-3-2]]))]
    result => #"\[0 1\] should be `integer\?`; it is `:elt-0-1`"
    result => #"\[1 0\] should be `integer\?`; it is `:elt-1-0`"
    result => #"\[3 2\] should be `integer\?`; it is `:elt-3-2`")

  (type! :Nesty {[:x ALL ALL :y] integer?})
  (let [result (with-out-str (checked :Nesty {:x [ [{:y 1}] [{:y :notint}]]}))]
    result => #"\[:x 1 0 :y\] should be `integer\?`; it is `:notint`")

  (let [result (with-out-str (checked :Nesty {:x [1]}))]
    result => #"\[:x ALL ALL :y\] is not a path"))

(start-over!)
