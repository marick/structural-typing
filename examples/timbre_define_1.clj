(ns timbre-define-1
  "Logging to Timbre"
  (:require [structural-typing.type :as type]
            [structural-typing.assist.oopsie :as oopsie]
            [taoensso.timbre :as timbre])
  ;; I know it's unfashionable, but in this case a separate `use` is clearer than :refer :all
  (:use [structural-typing.type :exclude [checked]]))

;; Example 1: not the greatest error reporting

(def type-repo
  (-> empty-type-repo
      (named :Point
             (requires :x :y)
             {:x integer? :y integer?})
      (replace-error-handler (oopsie/mkfn:apply-to-each-explanation #(timbre/error %)))))

(def checked (partial type/checked type-repo))

