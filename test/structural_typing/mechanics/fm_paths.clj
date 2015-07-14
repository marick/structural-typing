(ns structural-typing.mechanics.fm-paths
  (:require [structural-typing.mechanics.m-paths :as subject])
  (:require [com.rpl.specter :refer [ALL VAL]])
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

(fact path-will-match-many?
  (subject/path-will-match-many? [:a :b]) => false
  (subject/path-will-match-many? [:a ALL :b]) => true)

(fact replacement-points
  (subject/replacement-points [:a ALL :b ALL ALL]) => [1 3 4])

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

(fact replace-with-indices
  (subject/replace-with-indices [ALL ALL] [0 1] [17 3]) => [17 3]
  (subject/replace-with-indices [:a ALL :b ALL] [1 3] [17 3]) => [:a 17 :b 3])


(fact tag-many-matchers
  (subject/tag-many-matchers [:a :b]) => [:a :b]
  (subject/tag-many-matchers [ALL :a ALL]) => [VAL ALL :a VAL ALL]
  (subject/tag-many-matchers [:a ALL :b]) => [:a VAL ALL :b])
