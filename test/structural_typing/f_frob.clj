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

(facts nested-map->path-map
  (subject/nested-map->path-map {}) => {}
  (subject/nested-map->path-map {:a 1}) => {[:a] 1}
  (subject/nested-map->path-map {:a {:b [1]}}) => {[:a :b] [1]}


  (subject/nested-map->path-map {:a {:b {:c 3 :d 4}
                                     :e {:f 5}
                                     :g 6}
                                 :h 7})
  => {[:a :b :c] 3
      [:a :b :d] 4
      [:a :e :f] 5
      [:a :g] 6
      [:h] 7}

  (fact "for consistency with bouncer, keys that are vectors are assumed to be paths"
    (subject/nested-map->path-map {[:a :b] {:ignored :value}
                                   :l1 {[:l2 :l3] 3}})
    => {[:a :b] {:ignored :value}
        [:l1 :l2 :l3] 3}))
