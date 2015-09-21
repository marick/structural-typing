(ns structural-typing.guts.type-descriptions.f-ppps
  (:require [structural-typing.guts.type-descriptions.ppps :as subject]
            [structural-typing.guts.preds.core :refer [required-key]]
            [structural-typing.guts.type-descriptions.flatten :as flatten]
            [structural-typing.guts.type-descriptions.elements :as element])
  (:require [com.rpl.specter :refer [ALL]])
  (:use midje.sweet))

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
      (get result []) => (just (exactly even?))))

  (fact "about adding new required paths in presence of ANY and similar selectors"
    (fact relevant-subvectors
      (subject/relevant-subvectors []) => []
      (subject/relevant-subvectors [:x]) => []
      (subject/relevant-subvectors [:x :y]) => []
      (subject/relevant-subvectors [element/ALL]) => []
      (subject/relevant-subvectors [element/ALL :x]) => []
      (subject/relevant-subvectors [:x element/ALL :y]) => [[:x]]
      (subject/relevant-subvectors [:x :y element/ALL :z]) => [[:x :y]]
      (subject/relevant-subvectors [:x element/ALL :y element/ALL]) => [[:x] [:x element/ALL :y]]
      (subject/relevant-subvectors [:x element/ALL :y1 :y2 element/ALL :z]) => [[:x] [:x element/ALL :y1 :y2]])


    (tabular 
      (fact "elements that will cause Specter to match-many can add new 'required-key' clauses"
        ;; Easier to test this behavior of ->type-description directly
      (subject/add-implied-required-keys {?path #{required-key}}) => ?expected)
      ?path                              ?expected
      [:a :b]                            {[:a :b] #{required-key}}
      [:a element/ALL]                   {[:a element/ALL] #{required-key}
                                          [:a]     #{required-key}}
      [:a element/ALL :b]                {[:a element/ALL :b] #{required-key}
                                          [:a] #{required-key}}
      [:a element/ALL :b element/ALL]    {[:a element/ALL :b element/ALL] #{required-key}
                                          [:a] #{required-key}
                                          [:a element/ALL :b] #{required-key}}
      [:a element/ALL :b element/ALL :c] {[:a element/ALL :b element/ALL :c] #{required-key}
                                          [:a] #{required-key}
                                          [:a element/ALL :b] #{required-key}}
      [:a :b element/ALL :c :d]          {[:a :b element/ALL :c :d] #{required-key}
                                          [:a :b] #{required-key}}

      ;; subpaths ending in `will-match-many` don't make sense
      [:a :b element/ALL element/ALL]    {[:a :b element/ALL element/ALL] #{required-key}
                                          [:a :b] #{required-key}}
      [:a :b element/ALL element/ALL :c] {[:a :b element/ALL element/ALL :c] #{required-key}
                                                       [:a :b] #{required-key}}
      )

    (fact "such elements are not added when there is no match-many element in path"
      (subject/add-implied-required-keys {[:a :b] #{required-key}}) => {[:a :b] #{required-key}})

    (fact "such elements are not added when there are no required keys in the predicates"
      (subject/add-implied-required-keys {[:a element/ALL] #{even?}}) => {[:a element/ALL] #{even?}})
    
    (fact "addition of required keys adds on to previous elements (merge-with)"
      (subject/->type-description [(subject/->PPP [:a] [even?])
                                   (subject/->PPP [:a element/ALL :b]  [required-key])])
      => {[:a] [required-key even?]
          [:a element/ALL :b] [required-key]})))

