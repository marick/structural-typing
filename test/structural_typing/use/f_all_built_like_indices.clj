(ns structural-typing.use.f-all-built-like-indices
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil)
  (:refer-clojure :except [any?]))


(start-over!)

(fact "the results have paths"
  (check-all-for-explanations string? [100 "foo" :bar]) => (just (err:shouldbe [0] "string?" 100)
                                                                 (err:shouldbe [2] "string?" :bar))

  (check-all-for-explanations {ALL string?} [ [100] ["foo" :bar] ])
  => (just (err:shouldbe [0 0] "string?" 100)
           (err:shouldbe [1 1] "string?" :bar))


  (check-all-for-explanations [(requires [:x ALL :a]) {:y integer?}]
                              [ {:x [{:a 3}]}         ; 0
                                {:x [{:b 3}], :y 3}   ; 1
                                {:x "string"}         ; 2
                                {:y "string"}         ; 3
                                {:y 1}])              ; 4
  => (just (err:missing [1 :x 0 :a])
           (err:not-collection [2 :x ALL] "string")
           (err:missing [3 :x])
           (err:shouldbe [3 :y] "integer?" "\"string\"")
           (err:missing [4 :x])))

(start-over!)
