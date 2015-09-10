(ns structural-typing.guts.type-descriptions.f-paths
  (:require [structural-typing.guts.type-descriptions.paths :as subject]
            [structural-typing.guts.type-descriptions.dc-type-maps :as dc-type-map])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))

(fact "convert a simple path into paths"
  (subject/->paths []) => [[]]
  (subject/->paths [:a]) => [ [:a] ]
  (subject/->paths [:a :b]) => [ [:a :b] ])

(fact "forking paths"
  (fact "some synonyms"
    (subject/->paths [:a (subject/->Fork [ [:b1] [:b2]]) :c]) => [ [:a :b1 :c] [:a :b2 :c] ]
    (subject/->paths [:a (subject/through-each :b1 :b2) :c]) => [ [:a :b1 :c] [:a :b2 :c] ]
    (subject/->paths [:a (subject/each-of :b1 :b2) :c]) => [ [:a :b1 :c] [:a :b2 :c] ])

  (fact "can stand alone as a path itself"
    (subject/->paths (subject/each-of :b1 :b2)) => [ [:b1] [:b2] ])

  (fact "forks within forks"
    (subject/->paths [:a
                       (subject/through-each [:b1 (subject/through-each :b2a :b2b)]
                                              :c)
                       :d])
    => [ [:a :b1 :b2a :d]
         [:a :b1 :b2b :d]
         [:a :c :d] ]))

(fact "extracting paths from maps and types"
  (fact "an explicit map is just substituted"
    (let [point {[:x] [integer?], [:y] [integer?]}]
      (subject/->paths [:a (subject/paths-of point)]) => [ [:a :x] [:a :y] ]))
  (fact "nested maps are flattened"
    (let [nested {:a 1 :b {:c 2 :d 3}}]
      (subject/->paths [(subject/paths-of nested) :z]) => [ [:a :z]
                                                              [:b :c :z]
                                                              [:b :d :z] ]))

  (fact "can take a keyword for later type sustitution"
    (let [point {[:x] [integer?], [:y] [integer?]}

          as-written [:a (subject/paths-of :Point)]
          type-substituted (dc-type-map/substitute {:Point point} as-written)]
      (subject/->paths type-substituted) => [ [:a :x] [:a :y] ])))
