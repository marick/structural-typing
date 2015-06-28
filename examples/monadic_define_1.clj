(ns monadic-define-1
  "Using an Either monad to separate mistyped from valid values"
  (:require [structural-typing.type :as type]
            [structural-typing.api.custom :as custom]
            [blancas.morph.monads :as m]))

;; Example 1: not the greatest error reporting

(def type-repo
  (-> type/empty-type-repo
      (type/named :Point
                  (type/requires :x :y)
                  {:x integer? :y integer?})
      (type/replace-success-handler m/right)
      (type/replace-error-handler (custom/mkfn:apply-to-explanation-collection m/left))))

(def checked (partial type/checked type-repo))

