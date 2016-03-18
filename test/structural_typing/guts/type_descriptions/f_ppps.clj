(ns structural-typing.guts.type-descriptions.f-ppps
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.type-descriptions.ppps :as subject]
            [structural-typing.guts.compile.compile-path :refer [ALL]]
            [structural-typing.guts.preds.pseudopreds :refer [required-path]]
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
    (let [result (subject/->type-description [ (subject/->PPP [:x] [even? odd?])
                                               (subject/->PPP [:y] [pos?])])]
      (get result [:x]) => vector?
      (get result [:x]) => (just [(exactly even?) (exactly odd?)] :in-any-order)
      (get result [:y]) => [pos?]))
  
  (fact "duplicate paths are combined"
    (let [result (subject/->type-description [ (subject/->PPP [:x] [even?])
                                               (subject/->PPP [:x] [odd?])])]
      (get result [:x]) => (just [(exactly even?) (exactly odd?)] :in-any-order)))
  
  (fact "duplicate predicates appear only once"
    (let [result (subject/->type-description [ (subject/->PPP [:x] [even?])
                                               (subject/->PPP [:x] [odd?])
                                               (subject/->PPP [:x] [odd?])])]
      (get result [:x]) => (just (exactly even?) (exactly odd?) :in-any-order)))

  (fact "the predicate list is a sorted by order in which predicates were mentioned"
    (let [result (subject/->type-description [ (subject/->PPP [:x] [even?])
                                               (subject/->PPP [:x] [odd?])
                                               (subject/->PPP [:x] [required-path])
                                               (subject/->PPP [:x] [integer?])
                                               (subject/->PPP [:x] [pos?])])]
      (get result [:x]) => [even? odd? required-path integer? pos?]))

  (fact "it allows empty paths"
    (let [result (subject/->type-description [ (subject/->PPP [] [even?]) ])]
      (get result []) => (just (exactly even?)))))
