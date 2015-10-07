(ns monadic-define-2
  "Logging to Timbre"
  (:require [structural-typing.type :as type]
            [structural-typing.preds :as preds]
            [structural-typing.assist.oopsie :as oopsie]
            [blancas.morph.monads :as m])
  ;; I know it's unfashionable, but in this case a separate `use` is clearer than :refer :all
  (:use [structural-typing.type :exclude [built-like all-built-like
                                          <>built-like <>all-built-like
                                          built-like?]]))

(defn add-whole-value [oopsies]
  (cons (:whole-value (first oopsies))
        (oopsie/explanations oopsies)))

(def type-repo (-> empty-type-repo
                   (named :Point
                          (requires :x :y)
                          {:x integer? :y integer?})
                   (named :OriginTriangle
                          (includes :Point)
                          {:x (complement zero?) :y (complement zero?)})
                   (replace-error-handler (comp m/left add-whole-value))
                   (replace-success-handler m/right)))

(def built-like (partial type/built-like type-repo))
(def all-built-like (partial type/all-built-like type-repo))
(def <>built-like (partial type/<>built-like type-repo))
(def <>all-built-like (partial type/<>all-built-like type-repo))
(def built-like? (partial type/built-like? type-repo))


