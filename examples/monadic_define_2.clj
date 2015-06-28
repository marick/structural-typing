(ns monadic-define-2
  "Using an Either monad to separate mistyped from valid values"
  (:require [structural-typing.type :as type]
            [structural-typing.api.custom :as custom]
            [blancas.morph.monads :as m]))

;; Example 1: not the greatest error reporting

(defn add-whole-value [oopsies]
  (cons (:whole-value (first oopsies))
        (custom/explanations oopsies)))

(def type-repo (-> type/empty-type-repo
                   (type/named :Point
                               (type/requires :x :y)
                               {:x integer? :y integer?})
                   (type/replace-error-handler (comp m/left add-whole-value))
                   (type/replace-success-handler m/right)))

(def checked (partial type/checked type-repo))

