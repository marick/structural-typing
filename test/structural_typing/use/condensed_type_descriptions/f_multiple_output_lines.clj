(ns structural-typing.use.condensed-type-descriptions.f-multiple-output-lines
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
      structural-typing.type
      structural-typing.global-type
      structural-typing.clojure.core
      structural-typing.assist.testutil))


(fact "output lines are sorted"
  (type! :Figure
         (requires :color)
         {[:points ALL :x] [required-key integer?]
          [:points ALL :y] [required-key integer?]})

  (check-for-explanations :Figure {:points [{:y 1} {:x 1 :y "2"}]})
  => (just (err:required :color)
           (err:required [:points 0 :x])
           (err:shouldbe [:points 1 :y] "integer?" "\"2\""))

  (check-for-explanations :Figure {:points {:x 1 :y 2}})
  => (just (err:required :color)
           (err:required [:points 0 :x])
           (err:required [:points 0 :y])
           (err:required [:points 1 :x])
           (err:required [:points 1 :y])))
    
