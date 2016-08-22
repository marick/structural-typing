(ns structural-typing.use.condensed-type-descriptions.f-all-of
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil)
  (:refer-clojure :except [any?]))


(start-over!)

(type! :Point (requires :x :y))
(type! :Colorful (requires :color))

(fact "although `all-of` is primarily for use by `implies`, it can actually be used anywhere"
  (fact "all-of can replace a sequence of requires"
    (type! :X (requires :x) (requires :y))
    (type! :A (pred/all-of (requires :x) (requires :y)))
    
    (check-for-explanations :X {}) => (just (err:missing :x) (err:missing :y))
    (check-for-explanations :A {}) => (just (err:missing :x) (err:missing :y)))
  
  (fact "all-of can obey `includes`"
    (type! :X (includes :Colorful) (includes :Point))
    (type! :A (pred/all-of (includes :Colorful) (includes :Point)))
    (check-for-explanations :X {}) => (just (err:missing :color)
                                            (err:missing :x)
                                          (err:missing :y))
    (check-for-explanations :A {}) => (check-for-explanations :X {})))

(fact "a more complicated example using `implies`"
  ;; Multiple levels of type-expansion happening here.
  ;; Note also that `all-of` can be completely outside of the body of `type!`
  (let [then-part (pred/all-of (includes :Colorful)
                               (requires :z)
                               {:z pos?}
                               {:secondary [required-path (includes :Colorful)]})]
    (type! :A (pred/implies (includes :Point) then-part)))

  (built-like :A {}) => {}
  (check-for-explanations :A {:x 1 :y 1}) => (just (err:missing :color)
                                                   (err:missing :secondary)
                                                   (err:missing :z)))

(start-over!)
