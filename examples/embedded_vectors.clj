(ns embedded-vectors
  (:require [structural-typing.type :as type]
            [structural-typing.global-type :as global-type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

(global-type/start-over!)
(namespace-state-changes (before :facts (accumulator/reset!)))

(def contains-points {:points [{:x 1, :y 1}, {:x 2, :y 2}]})

(def bad-contains-points {:points [{:x 1, :y "1"}, {:x 2, :y "2"}]})

(global-type/named! :point [:x :y] {:x #'integer?, :y #'integer?})

; (clojure.pprint/pprint @structural-typing.pipeline-stages/global-type-repo)

(global-type/named! :has-points [:points] {:points (type/each-is :point)})

; (clojure.pprint/pprint @structural-typing.pipeline-stages/global-type-repo)
(prn "turn the following into a test")
(type/checked :has-points {:points [{:x 1, :y "2"}, {:x 3 :y 3}]})


; (bouncer.core/validate identity {:a [1 2 3]} {:a (type/each-is :foo)})

(future-fact "each-is takes a type-repo")

(future-fact "works with sequentials")

(global-type/start-over!)
