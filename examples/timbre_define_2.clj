(ns timbre-define-2
  "Using an Either monad to separate mistyped from valid values"
  (:require [structural-typing.type :as type]
            [structural-typing.api.custom :as custom]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]))

(timbre/set-level! :info)

;; Example 2: print a stack trace following the last error.

(defn error-explainer [oopsies]
  (timbre/info "While checking this:")
  (timbre/info (str/trimr (with-out-str (pprint (:whole-value (first oopsies))))))
  (doseq [e (custom/explanations oopsies)] (timbre/info e))
  (timbre/error "Boundary type check failed"))

(def type-repo
  (-> type/empty-type-repo
      (type/named :Point
                  (type/requires :x :y)
                  {:x integer? :y integer?})
      (type/replace-error-handler error-explainer)))

(def checked (partial type/checked type-repo))

