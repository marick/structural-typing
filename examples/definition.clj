(ns definition
  "An example of creating your own type namespace"
  ;; Because this is all about tailoring structural-typing, the rare `:refer :all` is appropriate:
  (:use structural-typing.type)
  ;; Additional predefined predicates live here:
  (:require [structural-typing.preds :as pred])
  ;; They're not actually used in this example.
  )

(def type-repo
  (-> empty-type-repo
      (replace-error-handler throwing-error-handler)

      (named :Point
             (requires :x :y)
             {:x integer? :y integer?})))

;; Define namespace-local versions of the standard functions that default to this type-repo.
;; The standard functions are `built-like`, `all-built-like`, `<>built-like`,
;; `<>all-built-like`, and `built-like?`.

(ensure-standard-functions type-repo)

;; For example, clients can use this:
;;     (definition/built-like :Point x)
