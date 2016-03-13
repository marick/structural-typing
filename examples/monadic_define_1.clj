(ns monadic-define-1
  "Using an Either monad to separate mistyped from valid values"
  ;; Because this is all about tailoring structural-typing, the rare `:refer :all` is appropriate:
  (:use structural-typing.type)

  (:require [structural-typing.preds :as pred]
            [structural-typing.assist.oopsie :as oopsie]
            [blancas.morph.monads :as m]))

;; The following code is explained in the wiki at
;; https://github.com/marick/structural-typing/wiki/Using-the-Either-monad
;; See `monadic-use` for examples.

(def type-repo
  (-> empty-type-repo
      (named :Point
                  (requires :x :y)
                  {:x integer? :y integer?})
      (named :FormsTriangle
                  {:x (complement zero?) :y (complement zero?)})
      (replace-success-handler m/right)
      (replace-error-handler (oopsie/mkfn:apply-to-explanation-collection m/left))))

;; Define namespace-local versions of the standard functions that don't require
;; clients to mention this type-repo. Those standard functions are:
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
