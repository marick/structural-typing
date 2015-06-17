(ns structural-typing.f-bouncer-validators
  (:require [structural-typing.bouncer-validators :as subject])
  (:use midje.sweet))






(facts flatten-path-representation
  (subject/flatten-path-representation :x) => [:x]
  (subject/flatten-path-representation [:x]) => [[:x]]
  (subject/flatten-path-representation [:x :y]) => [[:x :y]]
  (subject/flatten-path-representation [:x :y]) => [[:x :y]]

  (subject/flatten-path-representation [:x [:y :z]]) => [[:x :y] [:x :z]]
  (subject/flatten-path-representation [:a :b [:c :d]]) =future=> [[:a :b :c] [:a :b :d]]

  (subject/flatten-path-representation [:a :b [:c :d] :e]) =future=> [[:a :b :c :e] [:a :b :d :e]]

  (subject/flatten-path-representation [:a :b [:c :d] :e [:f :g]])
  =future=> [[:a :b :c :e :f] 
             [:a :b :c :e :g] 
             [:a :b :d :e :f] 
             [:a :b :d :e :g] ]

  (fact "the embedded paths don't have to be vectors"
    (subject/flatten-path-representation [:x (list :y :z)]) => [[:x :y] [:x :z]]))
    

(facts flatten-N-path-representations
  (subject/flatten-N-path-representations [:x :y]) => [:x :y]
  (subject/flatten-N-path-representations [:x [:x :y]]) => [:x [:x :y]]
  (subject/flatten-N-path-representations [:x [:a [:b1 :b2] :c]]) => [:x [:a :b1 :c] [:a :b2 :c]])

