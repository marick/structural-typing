(ns structural-typing.mechanics.fm-maps
  (:require [structural-typing.mechanics.m-maps :as subject])
  (:use midje.sweet))

(fact flatten-maps
  (subject/flatten-map {}) => {}
  (fact "makes sure keys are converted to paths and values are vectorized"
    (subject/flatten-map {:a 1}) => {[:a] [1]}
    (subject/flatten-map {[:a] [1]}) => {[:a] [1]})
  
  (fact "flattens sub-maps"
    (subject/flatten-map {:a {:b 1}}) => {[:a :b] [1]}
    (subject/flatten-map {[:c :d] {:e 1}}) => {[:c :d :e] [1]}
    (subject/flatten-map {:f {[:g :h] {:i [3 4]
                                       [:j :k] 5}}}) => {[:f :g :h :i] [3 4]
                                                         [:f :g :h :j :k] [5]})
             

  (fact "when flattening causes duplicate paths, values are merged"
    (let [result (subject/flatten-map {[:a :b :c] 1
                                       :a {[:b :c] 2}
                                       [:a :b] {:c 3 :d 4}})]
      (keys result) => (just [:a :b :c] [:a :b :d] :in-any-order)
      (result [:a :b :c]) => (just [1 2 3] :in-any-order)
      (result [:a :b :d]) => [4]))

  (fact "forking paths are left alone"
    (subject/flatten-map {[:a [:b :c] :d] 1
                          [ [:b :c] ] {[[:d :e]] 2}})
    => {[:a [:b :c] :d]   [1]
        [[:b :c] [:d :e]] [2]})

  (fact "keys are restricted to atoms and sequences"
    (subject/flatten-map {[:a {:a 1}] pos?})
    => (throws #"A path used as a map key.*a 1")))
 
