(ns structural-typing.paths.f-substituting
  (:require [com.rpl.specter :as specter])
  (:require [structural-typing.paths.substituting :as subject])
  (:require [structural-typing.paths.elements :refer [ALL]])
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




(fact replace-with-indices
  (subject/replace-with-indices [ALL ALL] [0 1] [17 3]) => [17 3]
  (subject/replace-with-indices [:a ALL :b ALL] [1 3] [17 3]) => [:a 17 :b 3])


(fact index-collecting-splice
  (specter/select (subject/index-collecting-splice ALL) [:a :b :c]) => (just [0 :a]
                                                                             [1 :b]
                                                                             [2 :c])
  
  (let [path (concat [:a]
                     (subject/index-collecting-splice ALL)
                     [:b]
                     (subject/index-collecting-splice  ALL))]

    (specter/select path {:a [{:b      [:one      :two]} {:b [:three]} {:b []} {:b [:four]}]})  
    =>                          [ [0 0 :one] [0 1 :two]  [1 0 :three]          [3 0 :four]]


    (specter/select path {:a [{} {:c 1} {:b [:one       :two]}]})
    =>                                [ [2 0 :one] [2 1 :two]]))

