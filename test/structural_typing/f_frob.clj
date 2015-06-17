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
  (subject/nested-map->path-map {:a 1}) => {[:a] [1]}
  (subject/nested-map->path-map {:a {:b [1]}}) => {[:a :b] [1]}


  (subject/nested-map->path-map {:a {:b {:c 3 :d 4}
                                     :e {:f 5}
                                     :g 6}
                                 :h 7})
  => {[:a :b :c] [3]
      [:a :b :d] [4]
      [:a :e :f] [5]
      [:a :g] [6]
      [:h] [7]}

  (fact "for consistency with bouncer, keys that are vectors are assumed to be paths"
    (subject/nested-map->path-map {[:a :b] {:ignored :value}
                                   :l1 {[:l2 :l3] [3 4]}})
    => {[:a :b] [{:ignored :value}]
        [:l1 :l2 :l3] [3 4]}))


(facts flatten-path-representation
  (subject/flatten-path-representation :x) => [:x]
  (subject/flatten-path-representation [:x]) => [[:x]]
  (subject/flatten-path-representation [:x :y]) => [[:x :y]]
  (subject/flatten-path-representation [:x :y]) => [[:x :y]]

  (subject/flatten-path-representation [:x [:y :z]]) => [[:x :y] [:x :z]]
  (subject/flatten-path-representation [:a :b [:c :d]]) =future=> [[:a :b :c] [:a :b :d]]

  (subject/flatten-path-representation [:a :b [:c :d] :e]) =future=> [[:a :b :c :e] [:a :b :d :e]]

  (subject/flatten-path-representation [:a :b [:c :d] :e [:f :g]])
  => [[:a :b :c :e :f] 
             [:a :b :c :e :g] 
             [:a :b :d :e :f] 
             [:a :b :d :e :g] ]

  (fact "the embedded paths don't have to be vectors"
    (subject/flatten-path-representation [:x (list :y :z)]) => [[:x :y] [:x :z]]))
    

(facts flatten-N-path-representations
  (subject/flatten-N-path-representations [:x :y]) => [:x :y]
  (subject/flatten-N-path-representations [:x [:x :y]]) => [:x [:x :y]]
  (subject/flatten-N-path-representations [:x [:a [:b1 :b2] :c]]) => [:x [:a :b1 :c] [:a :b2 :c]])

