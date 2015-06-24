(ns structural-typing.f-tbd
  (:require [structural-typing.type :as type])
  (:use midje.sweet))



(future-fact "type/all-of and type/some-of and type/which-of")



(future-fact "should be able to use a point to name a type")
;; ; (type/named! :line [:start :end] {:start :point, :end :point})
  

(future-fact "throw an exception if there is no matching key in the repo")

(future-fact "(type/checked type-repo :point 1)) produces an error, not an exception")

(future-fact "guarded predicates [(->> even? (only-when pos?))]")

(future-fact "coercions")
