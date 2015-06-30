(ns monadic-define-2
  "Logging to Timbre"
  (:require [structural-typing.type :as type]
            [structural-typing.api.custom :as custom]
            [blancas.morph.monads :as m]))

(defn add-whole-value [oopsies]
  (cons (:whole-value (first oopsies))
        (custom/explanations oopsies)))

(def type-repo (-> type/empty-type-repo
                   (type/named :Point
                               (type/requires :x :y)
                               {:x integer? :y integer?})
                   (type/named :OriginTriangle
                               (type/includes :Point)
                               {:x (complement zero?) :y (complement zero?)})
                   (type/replace-error-handler (comp m/left add-whole-value))
                   (type/replace-success-handler m/right)))

(def checked (partial type/checked type-repo))

