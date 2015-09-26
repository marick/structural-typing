(ns structural-typing.use.condensed-type-descriptions.f-forking-paths
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)

(fact "forking paths in `requires`"
  (type! :X (requires [:a (through-each :b1 :b2) (each-of [:c1 :d1] [:c2 :d2]) :e]))
  (let [ok {:a {:b1 {:c1 {:d1 {:e 2}}
                     :c2 {:d2 {:e 2}}}
                :b2 {:c1 {:d1 {:e 2}}
                     :c2 {:d2 {:e 2}}}}}]
    (built-like :X ok) => ok)
  (check-for-explanations :X {}) => [(err:required [:a :b1 :c1 :d1 :e])
                                     (err:required [:a :b1 :c2 :d2 :e])
                                     (err:required [:a :b2 :c1 :d1 :e])
                                     (err:required [:a :b2 :c2 :d2 :e])])

(fact "`each-of` is an alternate to `through-each`"
  (type! :X {[:refpoint (each-of :x :y)] integer?})
  (check-for-explanations :X {:refpoint {:y "2"}})
  => [(err:shouldbe [:refpoint :y] "integer?" "\"2\"")])

(fact "through-each and each-of can be used as entire paths"
  (type! :X {(through-each :b1 :b2) even?})
  (type! :X! {(each-of :b1 :b2) even?})
  (type! :Y {[:b1] even?
             [:b2] even?})
  (description :X) => (description :X!)
  (description :X) => (description :Y)

  (fact "in `requires` as well"
    (type! :X (requires (through-each :b1 :b2)))
    (type! :X! (requires (each-of :b1 :b2)))
    (type! :Y {[:b1] required-key
               [:b2] required-key})
    (description :X) => (description :X!)
    (description :X) => (description :Y)))

(fact "paths-of can be used as an entire path"
  (type! :Point (requires :x :y))
  (type! :X {(paths-of :Point) integer?})
  (type! :X! {[(paths-of :Point)] integer?})
  (type! :Y {:x [integer?]
             :y [integer?]})
  (description :X) => (description :X!)
  (description :X) => (description :Y)

  (fact "in `requires` as well"
    (type! :X (requires (paths-of :Point)))
    (type! :X (requires (paths-of :Point)))
    (type! :Y {:x [required-key]
               :y [required-key]})
    (description :X) => (description :Y)))
    



(fact "using a preexisting type to provide a forking path in a requires"
  (type! :Point (requires :x :y :color))
  (type! :X (requires :a (paths-of :Point)))

  (let [ok {:x 1, :y 1, :color 2, :a 3}]
    (built-like :X ok) => ok)
  (check-for-explanations :X {:x 1 :y 1}) => [(err:required :a)
                                              (err:required :color)])

(fact "the keys may be embedded in a path"
  (type! :Point (requires :x :y :color))
  (type! :X (requires [:point (paths-of :Point)]))

  (let [ok {:point {:x 1, :y 1, :color 2}}]
    (built-like :X ok) => ok)
  (check-for-explanations :X {:point {:x 1 :y 1}}) => [(err:required [:point :color])])

(fact "the keys may come from an explicit map"
  (type! :X (requires [:point (paths-of {:x 1, :y 2, :color "red"})]))

  (let [ok {:point {:x 1, :y 1, :color 2}}]
    (built-like :X ok) => ok)
  (check-for-explanations :X {:point {:x 1 :y 1}}) => [(err:required [:point :color])])

(fact "A branch may follow an ALL"
  (type! :Figure {[:points ALL (each-of :x :y)] [required-key integer?]})
  (check-for-explanations :Figure {:points [{:x "1"}]})
  => (just (err:shouldbe [:points 0 :x] "integer?" "\"1\"")
           (err:required [:points 0 :y])))


(fact "forking paths may result in duplicates to merge"
  (type! :X (requires [:a :b1]) {[:a (each-of :b1 :b2)] integer?})
  (type! :Y {[:a :b1] [required-key integer?]
             [:a :b2] integer?})
  (description :X) => (description :Y))
  
(start-over!)
