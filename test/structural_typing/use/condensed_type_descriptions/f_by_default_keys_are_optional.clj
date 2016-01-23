(ns structural-typing.use.condensed-type-descriptions.f-by-default-keys-are-optional
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)


(type! :Point {:x [integer? pos?] :y [integer? pos?]})

(fact "above :Point description actually works"
  (check-for-explanations :Point {:x -1 :y 2}) => [(err:shouldbe :x "pos?" -1)]
  => (just #":x should be `pos\?`"))

(fact "observe how keys are optional"
  (map #(built-like :Point %) [{} {:x 1} {:y 1} {:x 1 :y 2}])
  => [{} {:x 1} {:y 1} {:y 2, :x 1}])

(fact "note that an explicitly nil key counts as optional"
  (built-like :Point {:x nil :y nil}) => {:x nil :y nil})

(fact "excess keys are fine"
  (map #(built-like :Point %) [{:x 1 :y 2}
                            {:x 1 :y 2 :z 3}
                            {:x 1 :y 2 :color "red"}])
  => [{:y 2, :x 1} {:y 2, :z 3, :x 1} {:y 2, :color "red", :x 1}])

(start-over!)
