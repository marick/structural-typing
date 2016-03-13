(ns monadic-define-2
  "Using an Either monad to separate mistyped from valid values"
  ;; Because this is all about tailoring structural-typing, the rare `:refer :all` is appropriate:
  (:use structural-typing.type)

  (:require [structural-typing.preds :as pred]
            [structural-typing.assist.oopsie :as oopsie]
            [blancas.morph.monads :as m]))

;;; `monadic-use` contains motivation for this variant.

;;; The error handler is called with a (non-empty) collection of oopsies that
;;; came from a particular structure (the "whole value").
;;; This function converts the oopsies into "explanations" (which is typical). 
;;; But it also adds the whole value to the front of the resulting collection.

(defn explanations-with-whole-value [oopsies]
  (cons (:whole-value (first oopsies))
        (oopsie/explanations oopsies)))

(def type-repo (-> empty-type-repo
                   (named :Point
                          (requires :x :y)
                          {:x integer? :y integer?})
                   (named :OriginTriangle
                          (includes :Point)
                          {:x (complement zero?) :y (complement zero?)})
                   (replace-success-handler m/right)
                   (replace-error-handler (comp m/left explanations-with-whole-value))))

;; Define namespace-local versions of the standard functions that don't require
;; clients to mention the type-repo. Those standard functions are:
;; `built-like`, `all-built-like`, `<>built-like`, `<>all-built-like`, and `built-like?`.

(ensure-standard-functions type-repo)

;; For example, clients can use this:
;;     (mytypes/built-like :Point x)

;; The above standard definitions are sometimes wrong. For example,
;; the following definition for `built-like?` won't work with monads:
;;
;;     (def built-like? (partial structural-typing.type/built-like? type-repo))
;;
;; Given the Maybe monad, we have to interpret a `right` as meaning success,
;; and a `left` as an error. So we have to override the standard definition:
(def built-like? (comp m/right? built-like))
