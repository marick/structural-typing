(ns structural-typing.guts.f-frob
  (:require [structural-typing.guts.frob :as subject])
  (:use midje.sweet))

(fact force-vector
  (subject/force-vector 1) => (vector 1)
  (subject/force-vector [1]) => (vector 1)
  (subject/force-vector '(1)) => (vector 1))

(fact adding-on
  (subject/adding-on [] 1) => [1]
  (subject/adding-on [1 2 3] 4) => [1 2 3 4]
  (subject/adding-on [1 2 3] [4]) => [1 2 3 4])

(fact alternately
  (subject/alternately inc dec [0 0 10 10]) => [1 -1 11 9]
  (subject/alternately + - [0 0 10 10] [1 10 20 30]) => [1 -10 30 -20])
