(ns structural-typing.guts.compile.f-to-specter-path
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.compile.to-specter-path :as subject]
            [structural-typing.guts.preds.wrap :as wrap]
            [com.rpl.specter :as specter])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))


(fact "compiling a path-traversal function"
  (fact "a common case"
    (let [path [:a :b]
          whole-value {:a {:b 1}}]
      ( (subject/compile path) whole-value) => (just (exval 1 path whole-value))))

  (fact "predicates that filter out"
    (let [path [:a odd?]]
      ( (subject/compile path) {:a 1}) => (just (exval 1 path {:a 1}))
      ( (subject/compile path) {:a 2}) => empty?))

  (fact "A simple use of ALL"
    (let [path [subject/ALL]]
      ( (subject/compile path) [100 200]) => (just (exval 100 [0] [100 200])
                                                   (exval 200 [1] [100 200]))))

  (fact "ALL and keywords"
    (let [path [:a subject/ALL :b]
          whole-value {:a [{:b :one} {:b :two}]}]
      ( (subject/compile path) whole-value) => (just (exval :one [:a 0 :b] whole-value)
                                                     (exval :two [:a 1 :b] whole-value))))

  (fact "RANGE"
    (let [path [(subject/RANGE 1 3)]
          whole-value [0 :a :b 3]]
      ( (subject/compile path) whole-value) => (just (exval :a [1] whole-value)
                                                     (exval :b [2] whole-value))))

  (fact "an empty path"
    (let [path []
          whole-value [0 :a :b 3]]
      ( (subject/compile path) whole-value) => (just (exval whole-value [] whole-value))))

  (fact "a path with specific indexes"
    (let [path [:a 1 :b]
          whole-value {:a [{:b :one} {:b :two}]}]
      ( (subject/compile path) whole-value) => (just (exval :two path whole-value))))

  (fact "combining specific indexes and ALL"
    (let [path [:a 1 subject/ALL]
          whole-value {:a [ [3] [:x :y]]}]
      ( (subject/compile path) whole-value) => (just (exval :x [:a 1 0] whole-value)
                                                     (exval :y [:a 1 1] whole-value))))

  (fact "nested ALL"
    (let [path [subject/ALL subject/ALL]
          whole-value [ [:a :b] [:c :d] ]]
      ( (subject/compile path) whole-value) => (just (exval :a [0 0] whole-value)
                                                     (exval :b [0 1] whole-value)
                                                     (exval :c [1 0] whole-value)
                                                     (exval :d [1 1] whole-value)))))



(fact mkfn:whole-value->oopsies
  (fact "constant paths"
    (let [f (subject/mkfn:whole-value->oopsies [:a] (wrap/lift even?))]
      (f {}) => []
      (f {:a 2}) => []
      (f {:a 1}) => (just (contains {:leaf-value 1, :path [:a]}))))

  (fact "indexed path"
    (let [f (subject/mkfn:whole-value->oopsies [:a subject/ALL] (wrap/lift even?))]
      (f {}) =future=> (just (contains {:leaf-value :halted-before-leaf-value-found}))
      (f {:a [2]}) => []
      (f {:a [2 1]}) => (just (contains {:leaf-value 1, :path [:a 1]}))))
    
  (future-fact "a broken path"
    (let [f (subject/mkfn:whole-value->oopsies [subject/ALL] (wrap/lift even?))
          results (f 1)
          oopsie (first results)]
      ((:explainer oopsie) oopsie) => (err:bad-all-target [subject/ALL] 1 1))))
    
      
(fact 'mkfn:range-element-selector
  (let [make-range (fn [inclusive-start exclusive-end]
                     {:inclusive-start inclusive-start
                      :exclusive-end exclusive-end})]

    (fact "no missing elements"
      (let [in [ [0 :zero] [1 :one] [2 :two] [3 :three] ]]
        ( (subject/mkfn:range-element-selector (make-range 0 2)) in) => [[0 :zero] [1 :one]]
        ( (subject/mkfn:range-element-selector (make-range 0 1)) in) => [[0 :zero]]
        ( (subject/mkfn:range-element-selector (make-range 0 0)) in) => []
        ( (subject/mkfn:range-element-selector (make-range 2 4)) in) => [[2 :two] [3 :three]]))
    
    (fact "missing elements are filled with nils"
      (let [range (make-range 1 4)]
        (future-fact "no preceding ALL or RANGE"
          (let [in [ [0 :irrelevant] [1 :one] [2 :two] ]]
            ( (subject/mkfn:range-element-selector range) in) => [ [1 :one] [2 :two] [3 nil] ]))))))
  
