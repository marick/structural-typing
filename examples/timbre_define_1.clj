(ns timbre-define-1
  "Logging to Timbre"
  (:require [structural-typing.type :as type]
            [structural-typing.preds :as preds]
            [structural-typing.assist.oopsie :as oopsie]
            [taoensso.timbre :as timbre])
  ;; I know it's unfashionable, but in this case a separate `use` is clearer than :refer :all
  (:use [structural-typing.type :exclude [built-like all-built-like
                                          <>built-like <>all-built-like
                                          built-like?]]))

;; Example 1: not the greatest error reporting

(def type-repo
  (-> empty-type-repo
      (named :Point
             (requires :x :y)
             {:x integer? :y integer?})
      (replace-error-handler (oopsie/mkfn:apply-to-each-explanation #(timbre/error %)))))

(def built-like (partial type/built-like type-repo))
(def all-built-like (partial type/all-built-like type-repo))
(def <>built-like #(type/<>built-like %1 type-repo %2))
(def <>all-built-like #(type/<>all-built-like %1 type-repo %2))
(def built-like? (partial type/built-like? type-repo))


