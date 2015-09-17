(ns structural-typing.use.condensed-type-descriptions.f-forking-paths
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(fact "forking paths in `requires`"
  (type! :X (requires [:a (through-each :b1 :b2) (each-of [:c1 :d1] [:c2 :d2]) :e]))
  (let [ok {:a {:b1 {:c1 {:d1 {:e 2}}
                     :c2 {:d2 {:e 2}}}
                :b2 {:c1 {:d1 {:e 2}}
                     :c2 {:d2 {:e 2}}}}}]
    (checked :X ok) => ok)
  (check-for-explanations :X {}) => [(err:required [:a :b1 :c1 :d1 :e])
                                     (err:required [:a :b1 :c2 :d2 :e])
                                     (err:required [:a :b2 :c1 :d1 :e])
                                     (err:required [:a :b2 :c2 :d2 :e])])

(fact "using a preexisting type to provide a forking path in a requires"
  (type! :Point (requires :x :y :color))
  (type! :X (requires :a (paths-of :Point)))

  (let [ok {:x 1, :y 1, :color 2, :a 3}]
    (checked :X ok) => ok)
  (check-for-explanations :X {:x 1 :y 1}) => [(err:required :a)
                                              (err:required :color)])

(fact "the keys may be embedded in a path"
  (type! :Point (requires :x :y :color))
  (type! :X (requires [:point (paths-of :Point)]))

  (let [ok {:point {:x 1, :y 1, :color 2}}]
    (checked :X ok) => ok)
  (check-for-explanations :X {:point {:x 1 :y 1}}) => [(err:required [:point :color])])

(fact "the keys may come from an explicit map"
  (type! :X (requires [:point (paths-of {:x 1, :y 2, :color "red"})]))

  (let [ok {:point {:x 1, :y 1, :color 2}}]
    (checked :X ok) => ok)
  (check-for-explanations :X {:point {:x 1 :y 1}}) => [(err:required [:point :color])])

