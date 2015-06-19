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

(fact "making a map with uniform keys"
  (subject/mkmap:all-keys-with-value [] 3) => {}
  (subject/mkmap:all-keys-with-value [:a] 3) => {:a 3}
  (subject/mkmap:all-keys-with-value [:a [:b]] 3) => {:a 3, [:b] 3})





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

  (fact "vector keys are spliced into the path"
    (subject/nested-map->path-map {[:a :b] {:k "value"}
                                   :l1 {[:l2 :l3] [:pred1 :pred2]}})
    => {[:a :b :k] ["value"]
        [:l1 :l2 :l3] [:pred1 :pred2]}))


(facts flatten-path-representation
  (subject/flatten-path-representation :x) => [:x]
  (subject/flatten-path-representation [:x]) => [[:x]]
  (subject/flatten-path-representation [:x :y]) => [[:x :y]]
  (subject/flatten-path-representation [:x :y]) => [[:x :y]]

  (subject/flatten-path-representation [:x [:y :z]]) => [[:x :y] [:x :z]]
  (subject/flatten-path-representation [:a :b [:c :d]]) => [[:a :b :c] [:a :b :d]]

  (subject/flatten-path-representation [:a :b [:c :d] :e]) => [[:a :b :c :e] [:a :b :d :e]]

  (subject/flatten-path-representation [:a :b [:c :d] :e [:f :g]])
  => [[:a :b :c :e :f] 
             [:a :b :c :e :g] 
             [:a :b :d :e :f] 
             [:a :b :d :e :g] ]

  
  (fact "using maps for their keys"
    (subject/flatten-path-representation {:a #'even? :b #'even?})
    => (just [:a] [:b] :in-any-order)
    (subject/flatten-path-representation {:a {:b #'even?} :c #'even?})
    => (just [:a :b] [:c] :in-any-order)
    (subject/flatten-path-representation [{:a #'even? :b #'even?}])
    => (just [:a] [:b] :in-any-order)

    (subject/flatten-path-representation [:x :y {[:a :c] #'even? :b #'even?}])
    => (just [:x :y :a :c] [:x :y :b] :in-any-order)

    (subject/flatten-path-representation [:x :y {:a {:c #'even?} :b #'even?}])
    => (just [:x :y :a :c] [:x :y :b] :in-any-order)

    (fact "such a map must be the last element in the vector"
      (subject/flatten-path-representation [:x :y {[:a :c] #'even? :b #'even?} :z])
      => (throws #"The map must be the last element")))

  (fact "the embedded paths don't have to be vectors"
    (subject/flatten-path-representation [:x (list :y :z)]) => [[:x :y] [:x :z]]))
    

(facts flatten-N-path-representations
  (subject/flatten-N-path-representations [:x :y]) => [:x :y]
  (subject/flatten-N-path-representations [:x [:x :y]]) => [:x [:x :y]]
  (subject/flatten-N-path-representations [:x [:a [:b1 :b2] :c]]) => [:x [:a :b1 :c] [:a :b2 :c]])

