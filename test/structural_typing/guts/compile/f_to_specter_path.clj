(ns structural-typing.guts.compile.f-to-specter-path
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.compile.to-specter-path :as subject]
            [structural-typing.guts.preds.wrap :as wrap]
            [com.rpl.specter :as specter]
            [structural-typing.guts.type-descriptions.elements :as element])
  (:use midje.sweet
        structural-typing.assist.special-words))

(fact path-will-match-many?
  (subject/path-will-match-many? [:a :b]) => false
  (subject/path-will-match-many? [:a ALL :b]) => true)

(tabular "compiling paths"
  (fact 
    (let [[compiled kind] (subject/compile ?path)]
      kind => ?kind
      (specter/compiled-select compiled ?value) => ?selected))
  ?kind          ?path                 ?value                      ?selected
  :constant-path []                    ..whole..                   [..whole..]
  :constant-path [:a :b]               {:a {:b 1}}                 [1]
  :constant-path [:a even?]            {:a 1}                      empty?
  :indexed-path  [:a element/ALL :b]   {:a [{:b :one} {:b :two}]}  [ [0 :one] [1 :two] ]
  :indexed-path  [(element/RANGE 1 3)] [0 :a :b 3]                 [[1 :a] [2 :b]]
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
      ((:explainer oopsie) oopsie) => "[ALL] is not a path into `1`")))
    
      
