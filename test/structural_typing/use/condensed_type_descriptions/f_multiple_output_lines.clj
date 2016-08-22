(ns structural-typing.use.condensed-type-descriptions.f-multiple-output-lines
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
      structural-typing.type
      structural-typing.global-type
      structural-typing.clojure.core
      structural-typing.assist.testutil)
  (:refer-clojure :except [any?]))



(start-over!)

(fact "output lines are sorted"
  (type! :Figure
         (requires :color)
         {[:points ALL :x] [required-path integer?]
          [:points ALL :y] [required-path integer?]})

  (check-for-explanations :Figure {:points [{:y 1} {:x 1 :y "2"}]})
  => (just (err:missing :color)
           (err:missing [:points 0 :x])
           (err:shouldbe [:points 1 :y] "integer?" "\"2\""))

  (check-for-explanations :Figure {:points [{}{}]})
  => (just (err:missing :color)
           (err:missing [:points 0 :x])
           (err:missing [:points 0 :y])
           (err:missing [:points 1 :x])
           (err:missing [:points 1 :y])))

(start-over!)

