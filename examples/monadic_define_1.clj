(ns monadic-define-1
  "Using an Either monad to separate mistyped from valid values"
  (:require [structural-typing.type :as type]
            [structural-typing.preds :as preds]
            [structural-typing.assist.oopsie :as oopsie]
            [blancas.morph.monads :as m]))

;; The following code is explained in the wiki at
;; https://github.com/marick/structural-typing/wiki/Using-the-Either-monad
;; See `monadic-use` for examples.

(def type-repo
  (-> type/empty-type-repo
      (type/named :Point
                  (type/requires :x :y)
                  {:x integer? :y integer?})
      (type/named :FormsTriangle
                  {:x (complement zero?) :y (complement zero?)})
      (type/replace-success-handler m/right)
      (type/replace-error-handler (oopsie/mkfn:apply-to-explanation-collection m/left))))

;; Define namespace-local versions of the standard functions that don't require
;; clients to mention the type-repo. Those standard functions are:
;; `built-like`, `all-built-like`, `<>built-like`, `<>all-built-like`, and `built-like?`.

(type/ensure-standard-functions type-repo)

;; For example, clients can use this:
;;     (mytypes/built-like :Point x)

;; The above standard definitions are sometimes wrong. For example,
;; the following definition for `built-like?` won't work with monads:
;;
;;     (def built-like? (partial type/built-like? type-repo))
;;
;; Given the Maybe monad, we have to interpret a `right` as meaning success,
;; and a `left` as an error. So we have to override the standard definition:
(def built-like? (comp m/right? built-like))

;; The other definitions will. They are:
;;
;; (def built-like (partial type/built-like type-repo))
;; (def all-built-like (partial type/all-built-like type-repo))
;; (def <>built-like #(type/<>built-like %1 type-repo %2))
;; (def <>all-built-like #(type/<>all-built-like %1 type-repo %2))
