(ns structural-typing.docs.f-wiki-what-can-be-in-a-path
 (:require [structural-typing.type :refer :all]
           [structural-typing.global-type :refer :all]
           [clojure.string :as str])
 (:use midje.sweet))

(start-over!)

(fact "keywords"
  (type! :X {[:a :b] integer?})
  (described-by? :X {:a {:b 1}}) => true)

(future-fact "ALL"
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

(future-fact "RANGE"
  (type! :ALL {[ALL] even?})
  (let [result (with-out-str (checked :ALL [:wrong 4 2 :wrong]))]
    result => #"\[0\] should be `even\?`; it is `:wrong`"
    result => #"\[3\] should be `even\?`; it is `:wrong`")

  (type! :R {[(RANGE 1 3)] even?})
  (checked :R [:wrong 4 2 :wrong]) => [:wrong 4 2 :wrong]

  (type! :R {[ALL :x (RANGE 1 3)] even?})
  (let [result (with-out-str (checked :R [ {:x [:ignored 4 2]} 
                                           {:x [:ignored 1 2 :ignored]}]))]
    result => #"\[1 :x 1\] should be `even\?`; it is `1`")

  (type! :SECOND-AND-THIRD {[(RANGE 1 3)] pos?})
  (checked :SECOND-AND-THIRD [:ignored 1 2]) => [:ignored 1 2]
  (let [result (with-out-str (checked :SECOND-AND-THIRD [:ignored 1]))]
    result => #"\[\(RANGE 1 3\)\] is not a path into `\[:ignored 1\]`"))
  
  

(future-fact "predicates"
  (type! :Odder {[ALL even?] neg?, [ALL odd?] pos?})
  (let [result (with-out-str (checked :Odder [4 -3 2 -1]))]
    result => #"\[0 even\?\] should be `neg\?`; it is `4`"
    result => #"\[2 even\?\] should be `neg\?`; it is `2`"
    result => #"\[1 odd\?\] should be `pos\?`; it is `-3`"
    result => #"\[3 odd\?\] should be `pos\?`; it is `-1`"))

(start-over!)
