(ns structural-typing.guts.type-descriptions.f-ppps
  (:require [structural-typing.guts.type-descriptions.ppps :as subject]
            [structural-typing.guts.compile.to-specter-path :refer [ALL]]
            [structural-typing.guts.preds.core :refer [required-path]]
            [structural-typing.guts.type-descriptions.flatten :as flatten])
  (:use midje.sweet))

(fact "ppps from `required`"
  (fact "solitary keywords"
    (subject/condensed-description->ppps (subject/requires :a :b))
    => (just (subject/->PPP [:a] [required-path])
             (subject/->PPP [:b] [required-path])))
  (fact "required paths"
    (subject/condensed-description->ppps (subject/requires [:a :b] [:c :d]))
    => (just (subject/->PPP [:a :b] [required-path])
             (subject/->PPP [:c :d] [required-path])))

  (fact "paths containing forks"
    (subject/condensed-description->ppps
     (subject/requires [:a (flatten/through-each :b1 [:b2a :b2b]) :c]))
    => (just (subject/->PPP [:a :b1 :c] [required-path])
             (subject/->PPP [:a :b2a :b2b :c] [required-path])))

  (fact "paths containing map-paths"
    (subject/condensed-description->ppps
     (subject/requires [:point (flatten/paths-of {:x {:color string? :loc integer?} :y integer?})]))
    => (just (subject/->PPP [:point :x :color] [required-path])
             (subject/->PPP [:point :x :loc] [required-path])
             (subject/->PPP [:point :y] [required-path])))

  (fact "map-paths standing alone"
    (subject/condensed-description->ppps
     (subject/requires (flatten/paths-of {:x {:color string? :loc integer?} :y integer?})))
    => (just (subject/->PPP [:x :color] [required-path])
             (subject/->PPP [:x :loc] [required-path])
             (subject/->PPP [:y] [required-path]))))


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
  (subject/condensed-description->ppps :a) => [(subject/->PPP [:a] [required-path])])

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
  
  (fact "the predicate list is a vector with required-path first (if present)"
    (let [result (subject/->type-description [ (subject/->PPP [:x] [even?])
                                               (subject/->PPP [:x] [odd?])
                                               (subject/->PPP [:x] [required-path])
                                               (subject/->PPP [:x] [integer?])
                                               (subject/->PPP [:x] [pos?])])]
      (first (get result [:x])) => (exactly required-path)))

  (fact "it allows empty paths"
    (let [result (subject/->type-description [ (subject/->PPP [] [even?]) ])]
      (get result []) => (just (exactly even?))))

  (fact "about adding new required paths in presence of ANY and similar selectors"
    (fact relevant-subvectors
      (subject/relevant-subvectors []) => []
      (subject/relevant-subvectors [:x]) => []
      (subject/relevant-subvectors [:x :y]) => []
      (subject/relevant-subvectors [ALL]) => []
      (subject/relevant-subvectors [ALL :x]) => []
      (subject/relevant-subvectors [:x ALL :y]) => [[:x]]
      (subject/relevant-subvectors [:x :y ALL :z]) => [[:x :y]]
      (subject/relevant-subvectors [:x ALL :y ALL]) => [[:x] [:x ALL :y]]
      (subject/relevant-subvectors [:x ALL :y1 :y2 ALL :z]) => [[:x] [:x ALL :y1 :y2]])


    (tabular 
      (fact "elements that will cause Specter to match-many can add new 'required-path' clauses"
        ;; Easier to test this behavior of ->type-description directly
      (subject/add-implied-required-paths {?path #{required-path}}) => ?expected)
      ?path                     ?expected
      [:a :b]                   {[:a :b] #{required-path}}
      [:a ALL]                  {[:a ALL] #{required-path}
                                 [:a]     #{required-path}}
      [:a ALL :b]               {[:a ALL :b] #{required-path}
                                 [:a] #{required-path}}
      [:a ALL :b ALL]           {[:a ALL :b ALL] #{required-path}
                                 [:a] #{required-path}
                                 [:a ALL :b] #{required-path}}
      [:a ALL :b ALL :c]        {[:a ALL :b ALL :c] #{required-path}
                                 [:a] #{required-path}
                                 [:a ALL :b] #{required-path}}
      [:a :b ALL :c :d]         {[:a :b ALL :c :d] #{required-path}
                                 [:a :b] #{required-path}}

      ;; subpaths ending in `will-match-many` don't make sense
      [:a :b ALL ALL]    {[:a :b ALL ALL] #{required-path}
                          [:a :b] #{required-path}}
      [:a :b ALL ALL :c] {[:a :b ALL ALL :c] #{required-path}
                          [:a :b] #{required-path}}
      )

    (fact "such elements are not added when there is no match-many element in path"
      (subject/add-implied-required-paths {[:a :b] #{required-path}}) => {[:a :b] #{required-path}})

    (fact "such elements are not added when there are no required keys in the predicates"
      (subject/add-implied-required-paths {[:a ALL] #{even?}}) => {[:a ALL] #{even?}})
    
    (fact "addition of required keys adds on to previous elements (merge-with)"
      (subject/->type-description [(subject/->PPP [:a] [even?])
                                   (subject/->PPP [:a ALL :b]  [required-path])])
      => {[:a] [required-path even?]
          [:a ALL :b] [required-path]})))

