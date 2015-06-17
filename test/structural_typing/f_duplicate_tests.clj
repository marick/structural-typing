(ns structural-typing.f-duplicate-tests
  "Tests that are no longer needed, because there are better tests elsewhere.
   No harm in retaining them until they break."
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator]
            [bouncer.core :as b]
            [bouncer.validators :as bv])
  (:use midje.sweet))

;; (namespace-state-changes (before :facts (accumulator/reset!)))

;; (fact "you can validate nested maps"


;;   (fact "nested maps"
;;     (let [type-repo (-> accumulator/type-repo
;;                         (type/named :sample-type [:string [:point :x] [:point :y]]
;;                                     {:string #'string?
;;                                      :point {:x #'integer?}
;;                                      :distance #'pos?}))]
;;       (type/checked type-repo :sample-type {:string "hi" :point {:x 1, :y 1} :distance 3}) => {:string "hi" :point {:x 1, :y 1} :distance 3}
;;       (type/checked type-repo :sample-type {:string "hi" :point {:x 1, :y 1}}) => {:string "hi" :point {:x 1, :y 1}}

;;       (type/checked type-repo :sample-type {:string "hi" :point {:x 1}}) => :failure-handler-called
;;       (accumulator/messages) => ["[:point :y] must be present and non-nil"]

;;       (type/checked type-repo :sample-type {:string 3 :point {:x 1, :y 1}}) => :failure-handler-called
;;       (accumulator/messages) => [":string should be `string?`; it is `3`"]
      
;;       (type/checked type-repo :sample-type {:string "hi" :point {:x 1.0, :y 1.0}}) => :failure-handler-called
;;       (accumulator/messages) => ["[:point :x] should be `integer?`; it is `1.0`"]

;;       (type/checked type-repo :sample-type {:string "hi" :point {:x 1, :y 1} :distance -3}) => :failure-handler-called
;;       (accumulator/messages) => [":distance should be `pos?`; it is `-3`"]

;;       (type/checked type-repo :sample-type {:string "hi" :point {:x 1, :y 1} :distance "foo"}) => :failure-handler-called
;;       (accumulator/messages) => [":distance should be `pos?`; it is `\"foo\"`"]

;;       ))
;;   (fact "optional args"
;;     (let [type-repo (-> accumulator/type-repo
;;                         (type/named :sample-type [:string [:point :x] [:point :y]]
;;                                     {:string #'string?
;;                                      [:point :x] #'integer?
;;                                      :distance #'pos?}))]
;;       (type/checked type-repo :sample-type {:string "hi" :point {:x 1, :y 1} :distance 3}) => {:string "hi" :point {:x 1, :y 1} :distance 3}
;;       (type/checked type-repo :sample-type {:string "hi" :point {:x 1, :y 1}}) => {:string "hi" :point {:x 1, :y 1}}

;;       (type/checked type-repo :sample-type {:string "hi" :point {:x 1}}) => :failure-handler-called
;;       (accumulator/messages) => ["[:point :y] must be present and non-nil"]

;;       (type/checked type-repo :sample-type {:string 3 :point {:x 1, :y 1}}) => :failure-handler-called
;;       (accumulator/messages) => [":string should be `string?`; it is `3`"]
      
;;       (type/checked type-repo :sample-type {:string "hi" :point {:x 1.0, :y 1.0}}) => :failure-handler-called
;;       (accumulator/messages) => ["[:point :x] should be `integer?`; it is `1.0`"]

;;       (type/checked type-repo :sample-type {:string "hi" :point {:x 1, :y 1} :distance -3}) => :failure-handler-called
;;       (accumulator/messages) => [":distance should be `pos?`; it is `-3`"]

;;       (type/checked type-repo :sample-type {:string "hi" :point {:x 1, :y 1} :distance "foo"}) => :failure-handler-called
;;       (accumulator/messages) => [":distance should be `pos?`; it is `\"foo\"`"])))
