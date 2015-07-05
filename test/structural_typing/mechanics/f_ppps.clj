(ns structural-typing.mechanics.f-ppps
  (:require [structural-typing.mechanics.ppps :as subject :refer [->ppp]]
            [structural-typing.api.path :as path]
            [structural-typing.api.predicates :as pred])
  (:require [com.rpl.specter :refer [ALL]])
  (:use midje.sweet))

(fact flatmaps->ppps 
  (subject/flatmaps->ppps [{[:a] [even?]
                            [:b] [odd?]}
                           {[:c] [pos?]}]) => (just (->ppp [:a] #{even?})
                                                    (->ppp [:b] #{odd?})
                                                    (->ppp [:c] #{pos?})
                                                    :in-any-order))
(fact fix-forked-paths
  (fact "leaves flat paths alone"
    (subject/fix-forked-paths []) => empty?
    (subject/fix-forked-paths [ (->ppp [:a] ..preds..) ]) => [ (->ppp [:a] ..preds..) ])

  (fact "produces new ppps for forking paths"
    (subject/fix-forked-paths [ (->ppp [:a [:b1 :b2] :c] ..preds..)
                                          (->ppp [:simple] ..more-preds..)])
    => (just (->ppp [:a :b1 :c] ..preds..)
             (->ppp [:a :b2 :c] ..preds..)
             (->ppp [:simple] ..more-preds..)))

  (future-fact "affects canonicalization"
    (subject/canonicalize ..t.. (path/requires :a [:b [:l1 :l2] :c] :d))
    => {[:a] [pred/required-key]
        [:b :l1 :c] [pred/required-key]
        [:b :l2 :c] [pred/required-key]
        [:d] [pred/required-key]}
    
    (subject/canonicalize ..t.. (path/requires [[:a :b]])) => {[:a] [pred/required-key]
                                                               [:b] [pred/required-key]}))
  

(fact fix-required-paths-with-collection-selectors
  (subject/fix-required-paths-with-collection-selectors []) => []

  (fact "leaves non-required paths alone"
    (let [in [ (->ppp [:a ALL] #{even?}) ]]
      (subject/fix-required-paths-with-collection-selectors in) => in))
  (future-fact "leaves paths with only keys alone"
    (let [in [ (->ppp [:a :b] #{pred/required-key}) ]]
      (subject/fix-required-paths-with-collection-selectors in) => in))

  (tabular 
    (fact "adds new ppps for subpaths of required paths"
      (let [original (->ppp ?path #{even? pred/required-key})]
        (subject/fix-required-paths-with-collection-selectors [original])
        => (cons original ?additions)))
    ?path                ?additions
    [:a ALL]             [(->ppp [:a] #{pred/required-key})]
    [:a ALL :b]          [(->ppp [:a] #{pred/required-key})]
    [:a ALL :b ALL]      [(->ppp [:a] #{pred/required-key})
                          (->ppp [:a ALL :b] #{pred/required-key})]
    [:a ALL :b ALL :c]   [(->ppp [:a] #{pred/required-key})
                          (->ppp [:a ALL :b] #{pred/required-key})]
    [:a :b ALL :c]       [(->ppp [:a :b] #{pred/required-key})]
    [:a :b ALL :c :d]    [(->ppp [:a :b] #{pred/required-key})]
    [:a :b ALL ALL]      [(->ppp [:a :b] #{pred/required-key})]
    [:a :b ALL ALL :c]   [(->ppp [:a :b] #{pred/required-key})]
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
    (let [result (subject/dc2:ppps->type-description [ (->ppp [:x] #{1 2})
                                                       (->ppp [:y] #{3})])]
      (get result [:x]) => vector?
      (get result [:x]) => (just 1 2 :in-any-order)
      (get result [:y]) => [3]))
                                             
  (fact "duplicate paths are combined"
    (let [result (subject/dc2:ppps->type-description [ (->ppp [:x] #{1})
                                                       (->ppp [:x] #{2})])]
      (get result [:x]) => (just 1 2 :in-any-order)))

  (fact "duplicate predicates appear only once"
    (let [result (subject/dc2:ppps->type-description [ (->ppp [:x] #{1})
                                                       (->ppp [:x] #{2})
                                                       (->ppp [:x] #{2})])]
      (get result [:x]) => (just 1 2 :in-any-order)))

  (fact "the predicate list is a vector with required-key first (if present)")
    (let [result (subject/dc2:ppps->type-description [ (->ppp [:x] #{even?})
                                                       (->ppp [:x] #{odd?})
                                                       (->ppp [:x] #{pred/required-key})
                                                       (->ppp [:x] #{integer?})
                                                       (->ppp [:x] #{pos?})])]
      (first (get result [:x])) => (exactly pred/required-key)))
  
