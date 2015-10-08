(ns structural-typing.use.condensed-type-descriptions.f-requires-mentioned-paths
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)

(fact "simple cases"
  (fact "no condensed type descriptions"
    (type! :X (requires-mentioned-paths))
    (built-like :X {}) => {}
    (built-like :X {:whatever 3}) => {:whatever 3})
  (fact "a simple key"
    (type! :X (requires-mentioned-paths {:x integer?}))
    (built-like :X {:x 3}) => {:x 3}
    (check-for-explanations :X {:x :not-int}) => (just (err:shouldbe :x "integer?" :not-int))
    (check-for-explanations :X {}) => (just (err:required :x)))
  (fact "a path"
    (type! :X (requires-mentioned-paths {:x {:y integer?}}))
    (check-for-explanations :X {:x 3}) => (just (err:notpath [:x :y] {:x 3}))
    (check-for-explanations :X {:x {:y :not-int}}) => (just (err:shouldbe [:x :y] "integer?" :not-int))
    (check-for-explanations :X {}) => (just (err:required [:x :y]))))
    


(fact "use with includes"
  (type! :Point {:x integer?, :y integer?})
  (type! :X (requires-mentioned-paths {[:points ALL] (includes :Point)}))

  (built-like :X {:points [{:x 1 :y 1}]}) => {:points [{:x 1 :y 1}]}
  (check-for-explanations :X {:points [{:x 1}]}) => (just (err:required [:points 0 :y]))
  
  (future-fact "use with an `implies` that uses `includes`"))

(fact "typical use is to force paths in a previous type to be required"
  (type! :P {:x integer? :y {:z string?}})
  (type! :X (requires-mentioned-paths (includes :P)))

  (built-like :X {:x 1 :y {:z "foo"}}) => {:x 1 :y {:z "foo"}}
  (check-for-explanations :X {:x 1 :y {}}) => (just (err:required [:y :z])))
  
(fact "wrapping a part of group of condensed type descriptions"
  (type! :Point {:x integer? :y integer?})
  (type! :X
         (requires-mentioned-paths (includes :Point))
         (requires-mentioned-paths {:color string? :hue string?})
         {:other integer?})
  (let [in {:x 1 :y 2 :color "red" :hue "dark"}]
    (built-like :X in) => in)
  (check-for-explanations :X {}) => (just (err:required :color)
                                          (err:required :hue)
                                          (err:required :x)
                                          (err:required :y))
  (check-for-explanations :X {:x 1 :y 2 :color "red" :hue "dark" :other :non-int})
  => (just (err:shouldbe :other "integer?" :non-int)))

(fact "duplicate required-key are not affected"
  (type! :X (requires-mentioned-paths (requires :x)
                                {:x [integer?] :y [required-key]}
                                (requires :z)))
  (built-like :X {:x 3 :y 4 :z 5}) => {:x 3 :y 4 :z 5}
  (check-for-explanations :X {:x :not-int}) => (just (err:shouldbe :x "integer?" :not-int)
                                                     (err:required :y)
                                                     (err:required :z))
  (check-for-explanations :X {}) => (just (err:required :x)
                                          (err:required :y)
                                          (err:required :z)))

(start-over!)

