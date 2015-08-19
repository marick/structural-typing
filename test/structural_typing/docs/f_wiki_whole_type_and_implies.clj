(ns structural-typing.docs.f-wiki-whole-type-and-implies
 (:require [structural-typing.type :refer :all]
           [structural-typing.global-type :refer :all]
           [structural-typing.preds :as pred])
 (:use midje.sweet))

(start-over!)

(type! :String string?)
(checked :String 1)

(fact
  (checked :String "foo") => "foo"
  (with-out-str (checked :String nil)) => #"Value is nil, and that makes Sir Tony Hoare sad"
  (with-out-str (checked :String 1)) => #"Value should be `string")
  
(start-over!)
