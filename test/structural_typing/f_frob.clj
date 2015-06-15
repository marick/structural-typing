(ns structural-typing.f-frob
  (:require [structural-typing.frob :as subject])
  (:use midje.sweet))

(fact update-each-value
  (subject/update-each-value {} inc) => {}
  (subject/update-each-value {:a 1, :b 2} inc) => {:a 2 :b 3}
  (subject/update-each-value {:a [], :b [:b]} conj 1) => {:a [1] :b [:b 1]})

(fact wrap-pred-with-catcher
  (let [wrapped (subject/wrap-pred-with-catcher even?)]
    (wrapped 2) => true
    (wrapped 3) => false
    (even? nil) => (throws)
    (wrapped nil) => false))
  
(fact force-vector
  (subject/force-vector 1) => (vector 1)
  (subject/force-vector [1]) => (vector 1)
  (subject/force-vector '(1)) => (vector 1))

