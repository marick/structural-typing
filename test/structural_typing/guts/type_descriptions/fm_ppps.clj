(ns structural-typing.guts.type-descriptions.fm-ppps
  (:require [structural-typing.guts.type-descriptions.m-ppps :as subject :refer [->ppp]]
            [structural-typing.guts.preds.core :refer [required-key]]
            [structural-typing.guts.type-descriptions.flatten :as flatten])
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




;;;;;; NEW

(fact (pr-str (subject/requires :a :b [:c :d])) => "(required :a :b [:c :d])")

(fact "ppps from `required`"
  (fact "solitary keywords"
    (subject/condensed-description->ppps (subject/requires :a :b))
    => (just (subject/->PPP [:a] [required-key])
             (subject/->PPP [:b] [required-key])))
  (fact "required paths"
    (subject/condensed-description->ppps (subject/requires [:a :b] [:c :d]))
    => (just (subject/->PPP [:a :b] [required-key])
             (subject/->PPP [:c :d] [required-key])))

  (fact "paths containing forks"
    (subject/condensed-description->ppps
     (subject/requires [:a (flatten/through-each :b1 [:b2a :b2b]) :c]))
    => (just (subject/->PPP [:a :b1 :c] [required-key])
             (subject/->PPP [:a :b2a :b2b :c] [required-key])))

  (fact "paths containing map-paths"
    (subject/condensed-description->ppps
     (subject/requires [:point (flatten/paths-of {:x {:color string? :loc integer?} :y integer?})]))
    => (just (subject/->PPP [:point :x :color] [required-key])
             (subject/->PPP [:point :x :loc] [required-key])
             (subject/->PPP [:point :y] [required-key])))

  (fact "map-paths standing alone"
    (subject/condensed-description->ppps
     (subject/requires (flatten/paths-of {:x {:color string? :loc integer?} :y integer?})))
    => (just (subject/->PPP [:x :color] [required-key])
             (subject/->PPP [:x :loc] [required-key])
             (subject/->PPP [:y] [required-key]))))


(facts "ppps from maps"
  (fact "a simple canonicalized map produces a ppp for each key"
    (subject/condensed-description->ppps {[:a] [even?] [:b] [odd?]})
    => (just (subject/->PPP [:a] [even?])
             (subject/->PPP [:b] [odd?])
             :in-any-order))

  (fact "maps are canonicalized"
    (subject/condensed-description->ppps {:a even? :b odd?})
    => (just (subject/->PPP [:a] [even?])
             (subject/->PPP [:b] [odd?])
             :in-any-order)))

(facts "ppps from single keywords"
  (subject/condensed-description->ppps :a) => [(subject/->PPP [:a] [required-key])])

(facts "ppps from predicates"
  (subject/condensed-description->ppps even?) => [(subject/->PPP [] [even?])])
  
(facts "ppps from multimethods"
  (defmulti multimethod even?)
  (subject/condensed-description->ppps multimethod) => [(subject/->PPP [] [multimethod])])
  

(fact ->type-description
  (fact "it produces a map from ppps"
    (let [result (subject/->type-description [ (subject/->PPP [:x] [1 2])
                                               (subject/->PPP [:y] [3])])]
      (get result [:x]) => vector?
      (get result [:x]) => (just 1 2 :in-any-order)
      (get result [:y]) => [3]))
  
  (fact "duplicate paths are combined"
    (let [result (subject/->type-description [ (subject/->PPP [:x] [1])
                                               (subject/->PPP [:x] [2])])]
      (get result [:x]) => (just 1 2 :in-any-order)))
  
  (fact "duplicate predicates appear only once"
    (let [result (subject/->type-description [ (subject/->PPP [:x] [1])
                                               (subject/->PPP [:x] [2])
                                               (subject/->PPP [:x] [2])])]
      (get result [:x]) => (just 1 2 :in-any-order)))
  
  (fact "the predicate list is a vector with required-key first (if present)"
    (let [result (subject/->type-description [ (subject/->PPP [:x] [even?])
                                               (subject/->PPP [:x] [odd?])
                                               (subject/->PPP [:x] [required-key])
                                               (subject/->PPP [:x] [integer?])
                                               (subject/->PPP [:x] [pos?])])]
      (first (get result [:x])) => (exactly required-key)))

  (fact "it allows empty paths"
    (let [result (subject/->type-description [ (subject/->PPP [] [even?]) ])]
      (get result []) => (just (exactly even?)))))


