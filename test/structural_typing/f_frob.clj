(ns structural-typing.f-frob
  (:require [structural-typing.frob :as subject])
  (:use midje.sweet))

(fact force-vector
  (subject/force-vector 1) => (vector 1)
  (subject/force-vector [1]) => (vector 1)
  (subject/force-vector '(1)) => (vector 1))

(fact adding-on
  (subject/adding-on [] 1) => [1]
  (subject/adding-on [1 2 3] 4) => [1 2 3 4]
  (subject/adding-on [1 2 3] [4]) => [1 2 3 4])
