(ns structural-typing.mechanics.fm-paths
  (:require [structural-typing.mechanics.m-paths :as subject])
  (:require [com.rpl.specter :refer [ALL]])
  (:use midje.sweet))

(fact ends-in-map?
  (fact "maps are allows in some sequences"
    (subject/ends-in-map? [:a {:b 1}]) => true
    (subject/ends-in-map? [:a :b]) => false)

  (fact "error cases"
    (subject/ends-in-map? [{:a 1}])
    => (throws "A map cannot be the first element of a path: `[{:a 1}]`")

    (subject/ends-in-map? [:a {:a even?} :a])
    => (throws #"Nothing may follow a map within a path")))

(fact break-down-leaf-index
  (subject/break-down-leaf-index [3] 0) => [0]
  (subject/break-down-leaf-index [3] 1) => [1]
  (subject/break-down-leaf-index [3] 2) => [2]

  (subject/break-down-leaf-index [3 2] 0) => [0 0]
  (subject/break-down-leaf-index [3 2] 1) => [0 1]
  (subject/break-down-leaf-index [3 2] 2) => [1 0]
  (subject/break-down-leaf-index [3 2] 3) => [1 1]
  (subject/break-down-leaf-index [3 2] 4) => [2 0]
  (subject/break-down-leaf-index [3 2] 5) => [2 1]

  (subject/break-down-leaf-index [5 3 2] 0) => [0 0 0]
  (subject/break-down-leaf-index [5 3 2] 1) => [0 0 1]
  (subject/break-down-leaf-index [5 3 2] 2) => [0 1 0]
  (subject/break-down-leaf-index [5 3 2] 5) => [0 2 1]
  (subject/break-down-leaf-index [5 3 2] 6) => [1 0 0]
  (subject/break-down-leaf-index [5 3 2] 11) => [1 2 1]
  (subject/break-down-leaf-index [5 3 2] 12) => [2 0 0]
  (subject/break-down-leaf-index [5 3 2] 29) => [4 2 1]

  (fact "can take offsets"
    (subject/break-down-leaf-index [5 3 2] 12) => [2 0 0]
    (subject/break-down-leaf-index [5 3 2] 12
                                   [10 20 30]) => [12 20 30]))
