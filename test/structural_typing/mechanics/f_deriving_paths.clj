(ns structural-typing.mechanics.f-deriving-paths
  (:require [structural-typing.mechanics.deriving-paths :as subject]
            [structural-typing.api.path :as path])
  (:require [com.rpl.specter :refer [ALL]])
  (:use midje.sweet))


(facts "`from-forked-paths` converts a single vector into a vector of paths."
        
  (fact "single-argument form"
    (subject/from-forked-paths [:x]) => [[:x]]
    (subject/from-forked-paths [:x :y]) => [[:x :y]]
    (subject/from-forked-paths [:x :y]) => [[:x :y]]
    
    (subject/from-forked-paths [:x [:y :z]]) => [[:x :y] [:x :z]]
    (subject/from-forked-paths [:a :b [:c :d]]) => [[:a :b :c] [:a :b :d]]
    
    (subject/from-forked-paths [:a :b [:c :d] :e]) => [[:a :b :c :e] [:a :b :d :e]]
    
    (subject/from-forked-paths [:a :b [:c :d] :e [:f :g]])
    => [[:a :b :c :e :f] 
        [:a :b :c :e :g] 
        [:a :b :d :e :f] 
        [:a :b :d :e :g] ]

    (fact "note that `forks` is a synonym"
      (subject/from-forked-paths [:x (path/forks :y1 :y2)]) => [[:x :y1] [:x :y2]])
    
    (fact "maps should have been removed before this is called"
      (subject/from-forked-paths [{:a even? :b even?}]) => (throws)
      (subject/from-forked-paths [:x :y {[:a :c] even? :b even?}]) => (throws)
      (subject/from-forked-paths [:x :y {:a {:c even?} :b even?}]) => (throws)
      (subject/from-forked-paths [:x :y {[:a :c] even? :b even?} :z]) => (throws))

    (fact "the embedded paths don't have to be vectors"
      (subject/from-forked-paths [:x (list :y :z)]) => [[:x :y] [:x :z]]))

  (fact "two argument forms"
    (subject/from-forked-paths [] [[]]) => [[]]
    (subject/from-forked-paths [:b] [[:a]]) => [[:a :b]]
    (subject/from-forked-paths [:b] [[1] [2]]) => [[1 :b] [2 :b]]

    (subject/from-forked-paths [:a [:b :c] [:d :e]] [[1]])
    => (just [1 :a :b :d] [1 :a :b :e] [1 :a :c :d] [1 :a :c :e])))

(fact "from-paths-with-collection-selectors"
  (tabular 
    (fact 
      (subject/from-paths-with-collection-selectors ?path) => ?derivatives)
    ?path                ?derivatives
    [:a ALL]             [ [:a] ]
    [:a ALL :b]          [ [:a] ]
    [:a ALL :b ALL]      [ [:a] [:a ALL :b] ]
    [:a ALL :b ALL :c]   [ [:a] [:a ALL :b] ]
    [:a :b ALL :c]       [ [:a :b] ]
    [:a :b ALL :c :d]    [ [:a :b] ]
    [:a :b ALL ALL]      [ [:a :b] ]
    [:a :b ALL ALL :c]   [ [:a :b] ]))

