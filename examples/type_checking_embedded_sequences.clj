(ns type-checking-embedded-sequences
  "If a value contains a sequence of maps, you can check each of the sequence entries"
  (:use midje.sweet)
  (:require [structural-typing.type :as type]
            [structural-typing.global-type :as global-type]
            [structural-typing.testutil.accumulator :as accumulator]))

;; (global-type/start-over!)
;; ;(global-type/set-failure-handler! accumulator/failure-handler) ; stash failures in an atom
;; (namespace-state-changes (before :facts (accumulator/reset!)))

;; (def contains-points {:points [{:x 1, :y 1}, {:x 2, :y 2}]})

;; (def bad-contains-points {:points [{:x 1, :y "1"}, {:x 2, :y "2"}]})

;; (global-type/named! :point [:x :y] {:x #'integer?, :y #'integer?})
;; (global-type/named! :has-points [:points] {:points (type/old-each-is :point)})

;; (future-fact
;;   (type/checked :point {:x 1})
;;   (type/checked :has-points contains-points) => contains-points
;;   (type/checked :has-points bad-contains-points) => :failure-handler-called
;;   (accumulator/messages) => ["foo"])

;; (future-fact "old-each-is takes a type-repo")

;; (future-fact "works with sequentials")

;; (global-type/start-over!)
