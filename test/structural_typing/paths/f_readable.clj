(ns structural-typing.paths.f-readable
  (:require [structural-typing.paths.readable :as subject])
  (:require [structural-typing.paths.elements :refer [ALL]])
  (:use midje.sweet))

(fact "friendly paths"
  (subject/friendly [:a]) => ":a"
  (subject/friendly [1]) => "[1]"
  (subject/friendly [:a :b]) => "[:a :b]"
  (subject/friendly [:a ALL :b]) => "[:a ALL :b]"
  (subject/friendly [:a even? :b]) => "[:a even? :b]"
  (subject/friendly [:a 0 :b]) => "[:a 0 :b]")

(future-fact "handle other specter path components, including plain functions and vars")

