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

(facts nested->paths
  (subject/nested->paths :x) => [:x]
  (subject/nested->paths [:x]) => [[:x]]
  (subject/nested->paths [:x :y]) => [[:x :y]]
  (subject/nested->paths [:x :y]) => [[:x :y]]

  (subject/nested->paths [:x [:y :z]]) => [[:x :y] [:x :z]]
  (subject/nested->paths [:a :b [:c :d]]) =future=> [[:a :b :c] [:a :b :d]]

  (subject/nested->paths [:a :b [:c :d] :e]) =future=> [[:a :b :c :e] [:a :b :d :e]]

  (subject/nested->paths [:a :b [:c :d] :e [:f :g]])
  =future=> [[:a :b :c :e :f] 
             [:a :b :c :e :g] 
             [:a :b :d :e :f] 
             [:a :b :d :e :g] ]

  (fact "the embedded paths don't have to be vectors"
    (subject/nested->paths [:x (list :y :z)]) => [[:x :y] [:x :z]]))
    

(facts expand-all-paths
  (subject/expand-all-paths [:x :y]) => [:x :y]
  (subject/expand-all-paths [:x [:x :y]]) => [:x [:x :y]]
  (subject/expand-all-paths [:x [:a [:b1 :b2] :c]]) => [:x [:a :b1 :c] [:a :b2 :c]])

(fact prepend-bouncer-result-path
  (let [bouncer-diagnostics {:x [{:path [:x] :message "1"}
                                 {:path [:x] :message "2"}]
                             :y [{:path [:y] :message "3"}]}
        updated-path {:x [{:path [888 :x] :message "1"}
                                 {:path [888 :x] :message "2"}]
                             :y [{:path [888 :y] :message "3"}]}]
    (subject/prepend-bouncer-result-path
     [888]
     [bouncer-diagnostics {:bouncer.core/errors bouncer-diagnostics :x 1 :y 2}])
    => [updated-path {:bouncer.core/errors updated-path :x 1 :y 2}]))
    
                             
                             
