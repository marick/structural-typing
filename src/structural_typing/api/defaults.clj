(ns structural-typing.api.defaults
  (:require [structural-typing.api.path :as path]))

(defn default-error-explainer [{:keys [path predicate-string leaf-value]}]
  (format "%s should be `%s`; it is `%s`"
          (path/friendly-path path)
          predicate-string
          (pr-str leaf-value)))

