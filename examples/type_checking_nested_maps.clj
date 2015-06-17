(ns type-checking-nested-maps
  "There are two ways to describe types nested within types (maps within maps)."
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

(namespace-state-changes (before :facts (accumulator/reset!)))

;; ;; Use of [:key1 :key2 ...] paths
;; ;; Each path to a key is explicitly described.
;; (def path-style-type-structural
;;   (-> accumulator/type-repo
;;       (type/named :sample-type [:color [:point :x] [:point :y]])))

;; ;; Adding value descriptions by adding a map with paths to keys.
;; (def path-style-type-values
;;   (-> accumulator/type-repo
;;       (type/named :sample-type [:color [:point :x] [:point :y]]
;;          {[:point :x] #'integer?
;;           [:point :y] #'integer?})))


;; ;; The above is inconvenient when multiple nested maps should be of the same type.
;; ;; Use of paths that embrace nesting: [:key1 [:key2-1 :key2-2]], where the nested
;; ;; sequence will come from an independent map.

;; (def point-type {:x #'integer? :y #'integer?})

;; (def embedded-style-required
;;   (-> accumulator/type-repo
;;       (type/named :sample-type [:color [:point (keys point-type)]]
;;                   {:point point-type})))

;; ;; In the following, `:point` is not required, but its value is
;; ;; constrained if it's provided.
;; (def embedded-style-optional
;;   (-> accumulator/type-repo
;;       (type/named :sample-type [:color :point] ; contents of point are optional
;;                   {:point point-type})))





;; ;;;;;;; Tests of the above


;; (fact path-style-type-structural
;;   ;; one coordinate missing
;;   (type/checked path-style-type-structural :sample-type {:color "red"
;;                                               :point {:y 1}}) => :failure-handler-called
;;   (accumulator/messages) => (just "[:point :x] must be present and non-nil")

;;   ;; As always, extra arguments are allowed
;;   (let [big {:color "red"
;;              :mixin "blue"
;;              :point {:x 1, :y 2, :z 3}}]
;;     (type/checked path-style-type-values :sample-type big) => big))

;; (fact path-style-type-values
;;   ;; bad value type
;;   (type/checked path-style-type-values :sample-type {:color "red"
;;                                               :point {:y 1
;;                                                       :x :wrong}}) => :failure-handler-called
;;   (accumulator/messages) => (just "[:point :x] should be `integer?`; it is `:wrong`"))


;; (fact embedded-style-required
;;   ;; :point missing entirely
;;   (type/checked embedded-style-required :sample-type {:color "red"}) => :failure-handler-called
;;   (accumulator/messages) => (just "[:point :x] must be present and non-nil"
;;                                   "[:point :y] must be present and non-nil"
;;                                   :in-any-order)
  
;;   ;; one coordinate missing
;;   (type/checked embedded-style-required :sample-type {:color "red"
;;                                               :point {:y 1}}) => :failure-handler-called
;;   (accumulator/messages) => (just "[:point :x] must be present and non-nil")
  
;;   ;; bad value type
;;   (type/checked embedded-style-required :sample-type {:color "red"
;;                                               :point {:y 1
;;                                                       :x :wrong}}) => :failure-handler-called
;;   (accumulator/messages) => (just "[:point :x] should be `integer?`; it is `:wrong`"))

;; (fact embedded-style-optional
;;   ;; one coordinate missing - that's OK
;;   (type/checked embedded-style-optional :sample-type {:color "red" :point {:y 1}})
;;   => {:color "red" :point {:y 1}}
  
;;   ;; bad value type
;;   (type/checked embedded-style-optional :sample-type {:color "red"
;;                                               :point {:y 1
;;                                                       :x :wrong}}) => :failure-handler-called
;;   (accumulator/messages) => (just "[:point :x] should be `integer?`; it is `:wrong`"))
