(ns structural-typing.use.f-all-built-like-indices
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)

(fact "the results have paths"
  (check-all-for-explanations string? [100 "foo" :bar]) => (just (err:shouldbe [0] "string?" 100)
                                                                 (err:shouldbe [2] "string?" :bar))

  (check-all-for-explanations {ALL string?} [ [100] ["foo" :bar] ])
  => (just (err:shouldbe [0 0] "string?" 100)
           (err:shouldbe [1 1] "string?" :bar))


  (check-all-for-explanations [(requires [:x ALL :a]) {:y integer?}]
                              [ {:x [{:a 3}]}
                                {:x [{:b 3}], :y 3}
                                {:y "string"}
                                {:y 1}])
  =future=> (just (err:required [1 :x 0 :a])
           (err:required [2 :x])
           (err:shouldbe [2 :y] "integer?" "\"string\"")
           (err:required [3 :x])))

(start-over!)
