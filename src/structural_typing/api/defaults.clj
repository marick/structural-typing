(ns structural-typing.api.defaults
  (:require [structural-typing.api.path :as path]))

(defn default-error-explainer [{:keys [predicate-string leaf-value] :as explanation}]
  (format "%s should be `%s`; it is `%s`"
          (path/friendly-path explanation)
          predicate-string
          (pr-str leaf-value)))

