(ns structural-typing.guts.compile.f-to-specter-path
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.compile.to-specter-path :as subject]
            [structural-typing.guts.preds.wrap :as wrap]
            [com.rpl.specter :as specter])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))

(tabular "compiling paths"
  (fact 
    (let [[compiled kind] (subject/compile ?path)]
      kind => ?kind
      (specter/compiled-select compiled ?value) => ?selected))
  ?kind          ?path                 ?value                      ?selected
  :constant-path []                    ..whole..                   [..whole..]
  :constant-path [:a :b]               {:a {:b 1}}                 [1]
  :constant-path [:a even?]            {:a 1}                      empty?
  :indexed-path  [:a ALL :b]           {:a [{:b :one} {:b :two}]}  [ [0 :one] [1 :two] ]
  :indexed-path  [(subject/RANGE 1 3)] [0 :a :b 3]                 [[1 :a] [2 :b]]
  :constant-path [:a 1 :b]             {:a [{:b :one} {:b :two}]}  [:two]
  :indexed-path  [:a 1 ALL]            {:a [ [3] [:x :y]]}         [[0 :x] [1 :y]]
  :indexed-path  [ALL ALL]             [ [:a :b] [:c :d] ]         [ [0 0 :a] [0 1 :b]
                                                                     [1 0 :c] [1 1 :d]]
  )


(fact replace-with-indices
  (fact "ALL"
    (subject/replace-with-indices [ALL ALL] [17 3]) => [17 3]
    (subject/replace-with-indices [:a ALL :b ALL] [17 3]) => [:a 17 :b 3])
  (fact "RANGE"
    (subject/replace-with-indices [(RANGE 3 100) ALL] [17 3]) => [17 3]
    (subject/replace-with-indices [:a ALL :b (RANGE 1 100)] [17 3]) => [:a 17 :b 3]))


(fact mkfn:whole-value->oopsies
  (fact "constant paths"
    (let [f (subject/mkfn:whole-value->oopsies [:a] (wrap/lift even?))]
      (f {}) => []
      (f {:a 2}) => []
      (f {:a 1}) => (just (contains {:leaf-value 1, :path [:a]}))))

  (fact "indexed path"
    (let [f (subject/mkfn:whole-value->oopsies [:a ALL] (wrap/lift even?))]
      (f {}) => []
      (f {:a [2]}) => []
      (f {:a [2 1]}) => (just (contains {:leaf-value 1, :path [:a 1]}))))
    
  (fact "a broken path"
    (let [f (subject/mkfn:whole-value->oopsies [ALL] (wrap/lift even?))
          results (f 1)
          oopsie (first results)]
      ((:explainer oopsie) oopsie) => (err:notpath [ALL] 1))))
    
      
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
        (fact "no preceding ALL or RANGE"
          (let [in [ [0 :irrelevant] [1 :one] [2 :two] ]]
            ( (subject/mkfn:range-element-selector range) in) => [ [1 :one] [2 :two] [3 nil] ]))))))
  
