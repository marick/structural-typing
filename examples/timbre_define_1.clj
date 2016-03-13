(ns timbre-define-1
  "Logging to Timbre"
  ;; Because this is all about tailoring structural-typing, the rare `:refer :all` is appropriate:
  (:use structural-typing.type)

  (:require [structural-typing.preds :as pred]
            [structural-typing.assist.oopsie :as oopsie]
            [taoensso.timbre :as timbre]))

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

(ensure-standard-functions type-repo)

;; For example, clients can use this:
;;     (nmytypes/built-like :Point x)
