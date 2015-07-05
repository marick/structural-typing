(ns structural-typing.mechanics.f-ppps
  (:require [structural-typing.mechanics.ppps :as subject :refer [mk]]
            [structural-typing.api.path :as path]
            [structural-typing.api.predicates :as pred])
  (:require [com.rpl.specter :refer [ALL]])
  (:use midje.sweet))


(facts "`flatten-forked-path` converts a single vector into a vector of paths."
        
  (fact "single-argument form"
    (subject/flatten-forked-path [:x]) => [[:x]]
    (subject/flatten-forked-path [:x :y]) => [[:x :y]]
    (subject/flatten-forked-path [:x :y]) => [[:x :y]]
    
    (subject/flatten-forked-path [:x [:y :z]]) => [[:x :y] [:x :z]]
    (subject/flatten-forked-path [:a :b [:c :d]]) => [[:a :b :c] [:a :b :d]]
    
    (subject/flatten-forked-path [:a :b [:c :d] :e]) => [[:a :b :c :e] [:a :b :d :e]]
    
    (subject/flatten-forked-path [:a :b [:c :d] :e [:f :g]])
    => [[:a :b :c :e :f] 
        [:a :b :c :e :g] 
        [:a :b :d :e :f] 
        [:a :b :d :e :g] ]

    (fact "note that `forks` is a synonym"
      (subject/flatten-forked-path [:x (path/forks :y1 :y2)]) => [[:x :y1] [:x :y2]])
    
    (fact "maps should have been removed before this is called"
      (subject/flatten-forked-path [{:a even? :b even?}]) => (throws)
      (subject/flatten-forked-path [:x :y {[:a :c] even? :b even?}]) => (throws)
      (subject/flatten-forked-path [:x :y {:a {:c even?} :b even?}]) => (throws)
      (subject/flatten-forked-path [:x :y {[:a :c] even? :b even?} :z]) => (throws))

    (fact "the embedded paths don't have to be vectors"
      (subject/flatten-forked-path [:x (list :y :z)]) => [[:x :y] [:x :z]]))

  (fact "two argument forms"
    (subject/flatten-forked-path [] [[]]) => [[]]
    (subject/flatten-forked-path [:b] [[:a]]) => [[:a :b]]
    (subject/flatten-forked-path [:b] [[1] [2]]) => [[1 :b] [2 :b]]

    (subject/flatten-forked-path [:a [:b :c] [:d :e]] [[1]])
    => (just [1 :a :b :d] [1 :a :b :e] [1 :a :c :d] [1 :a :c :e])))

(fact unfork-condensed-ppp-paths
  (fact "leaves flat paths alone"
    (subject/unfork-condensed-ppp-paths []) => empty?
    (subject/unfork-condensed-ppp-paths [ (mk [:a] ..preds..) ]) => [ (mk [:a] ..preds..) ])

  (fact "produces new ppps for forking paths"
    (subject/unfork-condensed-ppp-paths [ (mk [:a [:b1 :b2] :c] ..preds..)
                                          (mk [:simple] ..more-preds..)])
    => (just (mk [:a :b1 :c] ..preds..)
             (mk [:a :b2 :c] ..preds..)
             (mk [:simple] ..more-preds..)))

  (future-fact "affects canonicalization"
    (subject/canonicalize ..t.. (path/requires :a [:b [:l1 :l2] :c] :d))
    => {[:a] [pred/required-key]
        [:b :l1 :c] [pred/required-key]
        [:b :l2 :c] [pred/required-key]
        [:d] [pred/required-key]}
    
    (subject/canonicalize ..t.. (path/requires [[:a :b]])) => {[:a] [pred/required-key]
                                                               [:b] [pred/required-key]}))
  

(fact add-required-subpaths
  (subject/add-required-subpaths []) => []

  (fact "leaves non-required paths alone"
    (let [in [ (mk [:a ALL] #{even?}) ]]
      (subject/add-required-subpaths in) => in))
  (future-fact "leaves paths with only keys alone"
    (let [in [ (mk [:a :b] #{pred/required-key}) ]]
      (subject/add-required-subpaths in) => in))

  (tabular 
    (fact "adds new ppps for subpaths of required paths"
      (let [original (mk ?path #{even? pred/required-key})]
        (subject/add-required-subpaths [original])
        => (cons original ?additions)))
    ?path                ?additions
    [:a ALL]             [(mk [:a] #{pred/required-key})]
    [:a ALL :b]          [(mk [:a] #{pred/required-key})]
    [:a ALL :b ALL]      [(mk [:a] #{pred/required-key})
                          (mk [:a ALL :b] #{pred/required-key})]
    [:a ALL :b ALL :c]   [(mk [:a] #{pred/required-key})
                          (mk [:a ALL :b] #{pred/required-key})]
    [:a :b ALL :c]       [(mk [:a :b] #{pred/required-key})]
    [:a :b ALL :c :d]    [(mk [:a :b] #{pred/required-key})]
    [:a :b ALL ALL]      [(mk [:a :b] #{pred/required-key})]
    [:a :b ALL ALL :c]   [(mk [:a :b] #{pred/required-key})]
    )


  (future-fact "affects canonicalization"

    (subject/canonicalize ..t.. (path/requires [:a ALL :c]
                                               [:b :f ALL])
                                {:a even?}
                                {[:b :f ALL] even?})
    => {[:a ALL :c] [pred/required-key]
        [:b :f ALL] [pred/required-key even?]
        [:a]        [even? pred/required-key]
        [:b :f]     [pred/required-key]}))



(fact dc2:ppps->type-description
  (fact "it produces a map from ppps"
    (let [result (subject/dc2:ppps->type-description [ (mk [:x] #{1 2})
                                                       (mk [:y] #{3})])]
      (get result [:x]) => vector?
      (get result [:x]) => (just 1 2 :in-any-order)
      (get result [:y]) => [3]))
                                             
  (fact "duplicate paths are combined"
    (let [result (subject/dc2:ppps->type-description [ (mk [:x] #{1})
                                                       (mk [:x] #{2})])]
      (get result [:x]) => (just 1 2 :in-any-order)))

  (fact "duplicate predicates appear only once"
    (let [result (subject/dc2:ppps->type-description [ (mk [:x] #{1})
                                                       (mk [:x] #{2})
                                                       (mk [:x] #{2})])]
      (get result [:x]) => (just 1 2 :in-any-order)))

  (fact "the predicate list is a vector with required-key first (if present)")
    (let [result (subject/dc2:ppps->type-description [ (mk [:x] #{even?})
                                                       (mk [:x] #{odd?})
                                                       (mk [:x] #{pred/required-key})
                                                       (mk [:x] #{integer?})
                                                       (mk [:x] #{pos?})])]
      (first (get result [:x])) => (exactly pred/required-key)))
  
