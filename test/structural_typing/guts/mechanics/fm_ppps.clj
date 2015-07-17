(ns structural-typing.guts.mechanics.fm-ppps
  (:require [structural-typing.guts.mechanics.m-ppps :as subject :refer [->ppp]]
            [structural-typing.guts.preds.required-key :refer [required-key]])
  (:require [com.rpl.specter :refer [ALL]])
  (:use midje.sweet))

(fact dc:flatmaps->ppps 
  (subject/dc:flatmaps->ppps [{[:a] [even?]
                               [:b] [odd?]}
                              {[:c] [pos?]}]) => (just (->ppp [:a] #{even?})
                                                       (->ppp [:b] #{odd?})
                                                       (->ppp [:c] #{pos?})
                                                       :in-any-order)

  (fact "you must have only predicates in the predicate list"
    (subject/dc:flatmaps->ppps [ {[:points ALL] [required-key 5]} ])
    => (throws #"`5` is not a predicate.")))

                              
(fact dc:fix-forked-paths
  (fact "leaves flat paths alone"
    (subject/dc:fix-forked-paths []) => empty?
    (subject/dc:fix-forked-paths [ (->ppp [:a] ..preds..) ]) => [ (->ppp [:a] ..preds..) ])

  (fact "produces new ppps for forking paths"
    (subject/dc:fix-forked-paths [ (->ppp [:a [:b1 :b2] :c] ..preds..)
                                   (->ppp [:simple] ..more-preds..)])
    => (just (->ppp [:a :b1 :c] ..preds..)
             (->ppp [:a :b2 :c] ..preds..)
             (->ppp [:simple] ..more-preds..))))
  

(fact dc:fix-required-paths-with-collection-selectors
  (subject/dc:fix-required-paths-with-collection-selectors []) => []

  (fact "leaves non-required paths alone"
    (let [in [ (->ppp [:a ALL] #{even?}) ]]
      (subject/dc:fix-required-paths-with-collection-selectors in) => in))
  (fact "leaves paths with only keys alone"
    (let [in [ (->ppp [:a :b] #{required-key}) ]]
      (subject/dc:fix-required-paths-with-collection-selectors in) => in))

  (tabular 
    (fact "adds new ppps for subpaths of required paths"
      (let [original (->ppp ?path #{even? required-key})]
        (subject/dc:fix-required-paths-with-collection-selectors [original])
        => (cons original ?additions)))
    ?path                ?additions
    [:a ALL]             [(->ppp [:a] #{required-key})]
    [:a ALL :b]          [(->ppp [:a] #{required-key})]
    [:a ALL :b ALL]      [(->ppp [:a] #{required-key})
                          (->ppp [:a ALL :b] #{required-key})]
    [:a ALL :b ALL :c]   [(->ppp [:a] #{required-key})
                          (->ppp [:a ALL :b] #{required-key})]
    [:a :b ALL :c]       [(->ppp [:a :b] #{required-key})]
    [:a :b ALL :c :d]    [(->ppp [:a :b] #{required-key})]
    [:a :b ALL ALL]      [(->ppp [:a :b] #{required-key})]
    [:a :b ALL ALL :c]   [(->ppp [:a :b] #{required-key})]))




(fact ->type-description
  (fact "it produces a map from ppps"
    (let [result (subject/->type-description [ (->ppp [:x] #{1 2})
                                                       (->ppp [:y] #{3})])]
      (get result [:x]) => vector?
      (get result [:x]) => (just 1 2 :in-any-order)
      (get result [:y]) => [3]))
  
  (fact "duplicate paths are combined"
    (let [result (subject/->type-description [ (->ppp [:x] #{1})
                                               (->ppp [:x] #{2})])]
      (get result [:x]) => (just 1 2 :in-any-order)))
  
  (fact "duplicate predicates appear only once"
    (let [result (subject/->type-description [ (->ppp [:x] #{1})
                                               (->ppp [:x] #{2})
                                               (->ppp [:x] #{2})])]
      (get result [:x]) => (just 1 2 :in-any-order)))
  
  (fact "the predicate list is a vector with required-key first (if present)"
    (let [result (subject/->type-description [ (->ppp [:x] #{even?})
                                               (->ppp [:x] #{odd?})
                                               (->ppp [:x] #{required-key})
                                               (->ppp [:x] #{integer?})
                                               (->ppp [:x] #{pos?})])]
      (first (get result [:x])) => (exactly required-key))))

