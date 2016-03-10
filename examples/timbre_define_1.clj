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

;; Define namespace-local versions of the standard functions that default to this type-repo.
;; The standard functions are `built-like`, `all-built-like`, `<>built-like`,
;; `<>all-built-like`, and `built-like?`.

(type/ensure-standard-functions type-repo)

;; For example, clients can use this:
;;     (mytypes/built-like :Point x)
