(ns structural-typing.f-bouncer
  (:require [structural-typing.bouncer :as subject])
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


(fact "flatten-error-map makes nesting easier to deal with"
  (subject/flatten-error-map nil) => empty?
  (subject/flatten-error-map {}) => empty?
  (subject/flatten-error-map {:a ["a message" "a message 2"]}) => ["a message" "a message 2"]
  (subject/flatten-error-map {:a ["a message"]
                              :point {:x ["x wrong"]
                                      :y ["y wrong"]}
                              :deep {:er {:still ["still wrong"]}}})
  => (just "a message" "x wrong" "y wrong" "still wrong" :in-any-order))


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
    
                             
                             
