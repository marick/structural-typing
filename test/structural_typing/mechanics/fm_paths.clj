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

