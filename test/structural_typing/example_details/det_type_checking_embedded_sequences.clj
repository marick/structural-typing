(ns structural-typing.example-details.det-type-checking-embedded-sequences
  (:use midje.sweet)
  (:require [structural-typing.type :as type]
            [structural-typing.global-type :as global-type]
            [structural-typing.testutil.accumulator :as accumulator]))


;; (fact "single level `each-is`"
;;   (let [type-repo (-> accumulator/type-repo
;;                       (type/named :point [:x :y]  {:x #'integer? :y #'integer?}))]
;;     ( (type/each-is type-repo :point) []) => nil
;;     ( (type/each-is type-repo :point) 1) =future=> :some-error-message 
;;     ( (type/each-is type-repo :point) [{:x :key, :y 1} {:x 1}])
;;    => {0 {:x [":x should be `integer?`; it is `:key`"]}
;;        1 {:y [":y must be present and non-nil"]}}


;; ))

;; (future-fact "multi-level `each-is`"
;;   (let [type-repo (-> accumulator/type-repo
;;                       (type/named :point [:x :y]  {:x #'integer? :y #'integer?}))
;;         type-repo (type/named type-repo :line [:points :color]
;;                               {:points (type/each-is type-repo :point)})]

;;     ( (type/each-is type-repo :line) [{:color "red"}])
;;     => {0 {:points [":points must be present and non-nil"]}}

;;     ( (type/each-is type-repo :line) [{:points [{:x 1, :y 2}, {:x 1}] :color "red"}
;;                                       {:points [{:x 1, :y "3"}]}])
;;     => {0 {:points {1 [":y must be present and non-nil"]}}
;;         1 {:color [":color must be present and non-nil"]
;;            :points {0 [":y should be `integer?`; it is `\"3\"`"]}}}

;; ))
