(ns monadic-define-1
  "Using an Either monad to separate mistyped from valid values"
  (:require [structural-typing.type :as type]
            [structural-typing.pred-writing.oopsie :as oopsie]
            [blancas.morph.monads :as m])
  ;; I know it's unfashionable, but in this case a separate `use` is clearer than :refer :all
  (:use [structural-typing.type :exclude [checked]]))

;; Example 1: not the greatest error reporting

(def type-repo
  (-> empty-type-repo
      (named :Point
             (requires :x :y)
             {:x integer? :y integer?})
      (named :FormsTriangle
             {:x (complement zero?) :y (complement zero?)})
      (replace-success-handler m/right)
      (replace-error-handler (oopsie/mkfn:apply-to-explanation-collection m/left))))

(def checked (partial type/checked type-repo))

