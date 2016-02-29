(ns structural-typing.assist.f-format
  (:require [structural-typing.assist.format :as subject])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))

(defrecord R [a b])

(fact "record classes can be printed more nicely"
  (subject/record-class (->R 1 2)) => "R")

(fact "records are prettier if printed without the whole namespace"
  (subject/leaf:record (->R 1 2)) => "#R{:a 1, :b 2}")


(fact "friendly-path formats path vectors helpfully"
  (subject/friendly-path [:a]) => ":a"
  (subject/friendly-path [:a :b]) => "[:a :b]"
  (subject/friendly-path [:a ALL :b]) => "[:a ALL :b]"
  (subject/friendly-path []) => "Value"

  (fact "a path that is not a collection is assumed to have already been formatted"
    (subject/friendly-path ":a") => ":a"))
