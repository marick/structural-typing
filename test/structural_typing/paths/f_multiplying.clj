(ns structural-typing.paths.f-multiplying
  (:require [structural-typing.paths.multiplying :as subject])
  (:require [com.rpl.specter :refer [ALL]])
  (:require [structural-typing.paths.readability :refer [forks]])
  (:use midje.sweet))



(facts "`forked-paths` converts a single vector into a vector of paths."
        
  (fact "single-argument form"
    (subject/forked-paths [:x]) => [[:x]]
    (subject/forked-paths [:x :y]) => [[:x :y]]
    (subject/forked-paths [:x :y]) => [[:x :y]]
    
    (subject/forked-paths [:x [:y :z]]) => [[:x :y] [:x :z]]
    (subject/forked-paths [:a :b [:c :d]]) => [[:a :b :c] [:a :b :d]]
    
    (subject/forked-paths [:a :b [:c :d] :e]) => [[:a :b :c :e] [:a :b :d :e]]
    
    (subject/forked-paths [:a :b [:c :d] :e [:f :g]])
    => [[:a :b :c :e :f] 
        [:a :b :c :e :g] 
        [:a :b :d :e :f] 
        [:a :b :d :e :g] ]

    (fact "note that `forks` is a synonym"
      (subject/forked-paths [:x (forks :y1 :y2)]) => [[:x :y1] [:x :y2]])
    
    (fact "maps should have been removed before this is called"
      (subject/forked-paths [{:a even? :b even?}]) => (throws)
      (subject/forked-paths [:x :y {[:a :c] even? :b even?}]) => (throws)
      (subject/forked-paths [:x :y {:a {:c even?} :b even?}]) => (throws)
      (subject/forked-paths [:x :y {[:a :c] even? :b even?} :z]) => (throws))

    (fact "the embedded paths don't have to be vectors"
      (subject/forked-paths [:x (list :y :z)]) => [[:x :y] [:x :z]]))

  (fact "two argument forms"
    (subject/forked-paths [] [[]]) => [[]]
    (subject/forked-paths [:b] [[:a]]) => [[:a :b]]
    (subject/forked-paths [:b] [[1] [2]]) => [[1 :b] [2 :b]]

    (subject/forked-paths [:a [:b :c] [:d :e]] [[1]])
    => (just [1 :a :b :d] [1 :a :b :e] [1 :a :c :d] [1 :a :c :e])))

(fact "required-prefix-paths"
  (tabular 
    (fact 
      (subject/required-prefix-paths ?path) => ?derivatives)
    ?path                ?derivatives
    [:a ALL]             [ [:a] ]
    [:a ALL :b]          [ [:a] ]
    [:a ALL :b ALL]      [ [:a] [:a ALL :b] ]
    [:a ALL :b ALL :c]   [ [:a] [:a ALL :b] ]
    [:a :b ALL :c]       [ [:a :b] ]
    [:a :b ALL :c :d]    [ [:a :b] ]
    [:a :b ALL ALL]      [ [:a :b] ]
    [:a :b ALL ALL :c]   [ [:a :b] ]))

