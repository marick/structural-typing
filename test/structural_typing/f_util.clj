(ns structural-typing.f-util
  (:require [structural-typing.util :as subject])
  (:use midje.sweet))

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
