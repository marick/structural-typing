(ns timbre-define-1
  "Logging to Timbre"
  (:require [structural-typing.type :as type]
            [structural-typing.api.custom :as custom]
            [taoensso.timbre :as timbre]))

;; Example 1: not the greatest error reporting

(def type-repo
  (-> type/empty-type-repo
      (type/named :Point
                  (type/requires :x :y)
                  {:x integer? :y integer?})
      (type/replace-error-handler
       (custom/mkfn:apply-to-each-explanation #(timbre/error %)))))

(def checked (partial type/checked type-repo))

