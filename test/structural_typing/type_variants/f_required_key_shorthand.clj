(ns structural-typing.type-variants.f-required-key-shorthand
  (:use structural-typing.type
        structural-typing.global-type)
  (:use midje.sweet structural-typing.assist.testutil))

(start-over!)

(fact "keys standing alone"
  (type! :X :x :y)
  (is-checked :X {:x 1, :y 1}) => true
  (check-for-explanations :X {}) => (just (err:required :x)
                                                  (err:required :y)))

(fact "a vector of keys"
  (type! :X [:x :y] [:z])
  (is-checked :X {:x 1, :y 1, :z 1}) => true
  (check-for-explanations :X {:z 1}) => (just (err:required :x)
                                                      (err:required :y))
  (check-for-explanations :X {:x 1, :y 2}) => (just (err:required :z)))

(fact "required paths"
  (type! :X [:x [:a :b]])
  (is-checked :X {:x 1, :a {:b 1}}) => true
  (check-for-explanations :X {:a {:b 2}}) => (just (err:required :x))
  (check-for-explanations :X {:x 1 :a 1 :b 2}) => (just (err:required [:a :b]))
  (check-for-explanations :X {:x 1 :a {:c 2}}) => (just (err:required [:a :b])))

(fact "alternate notation for required"
  (type! :X (requires :x [:a :b]))
  (is-checked :X {:x 1, :a {:b 1}}) => true
  (check-for-explanations :X {:x 1 :a 1 :b 2}) => (just (err:required [:a :b])))

(fact "forks inside of paths"
  (fact "simple case"
    (type! :X (requires [:a (forks :b :c) :d]))
    (is-checked :X {:x 1, :a {:b {:d 1}
                              :c {:d 2}}}) => true
    (check-for-explanations :X {:a {:b {:d 1}}}) => (just (err:required [:a :c :d]))
    (check-for-explanations  :X {:x 1, :a {:b {:d 1}
                                           :c {:e 2}}}) => (just (err:required [:a :c :d]))
    (description :X) => '{[:a :b :d] [required-key], [:a :c :d] [required-key]})

  (future-fact "forks that are paths themselves"
    (type! :Y (requires [:a (forks [:b1 :c1] [:b2 :c2]) :d]))
    (description :Y) => '{[:a :b1 :c1 :d] [required-key],
                          [:a :b2 :c2 :d] [required-key]}
    ;; todo try a few cases
    )

  (future-fact "forks within forks"))


(future-fact "paths-of"
  (type! :Point {:x integer?, :y integer?})
  (type! :X (requires (paths-of :Point)))
  (type! :X (requires [:point (paths-of :Point)])))

(future-fact "paths-of applied to map"
  "should canonicalize"
  "can have more path appended to the end?")


(start-over!)
