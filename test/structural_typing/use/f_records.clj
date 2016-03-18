(ns structural-typing.use.f-records
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)

(defrecord Top [middles])
(defrecord Middle [bottoms])
(defrecord Bottom [val])

(fact "records can be traversed by a path"
  (let [bottoms [(Bottom. 1) (Bottom. 2) (Bottom. 3) (Bottom. 4)
                 (Bottom. 5) (Bottom. 6) (Bottom. 7) (Bottom. 8)]
        middles [(Middle. [(nth bottoms 0) (nth bottoms 1)])
                 (Middle. [(nth bottoms 2) (nth bottoms 3)])
                 (Middle. [(nth bottoms 4) (nth bottoms 5)])
                 (Middle. [(nth bottoms 6) (nth bottoms 7)])]
        tops [(Top. [(nth middles 0) (nth middles 1)])
              (Top. [(nth middles 2) (nth middles 3)])]]
    (check-for-explanations {[ALL :middles ALL :bottoms ALL :val] even?} tops)
    => [(err:shouldbe [0 :middles 0 :bottoms 0 :val] "even?" 1)
        (err:shouldbe [0 :middles 1 :bottoms 0 :val] "even?" 3)
        (err:shouldbe [1 :middles 0 :bottoms 0 :val] "even?" 5)
        (err:shouldbe [1 :middles 1 :bottoms 0 :val] "even?" 7)]))

(start-over!)
