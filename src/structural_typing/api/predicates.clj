(ns structural-typing.api.predicates
  (:require [structural-typing.mechanics.m-lifting-predicates :as lift]
            [structural-typing.api.path :as path]))

;; Utilities

(defn show-as [name f]
  (-> f
      lift/stash-defaults
      (lift/replace-predicate-string name)))

(defn explain-with [explainer f]
  (-> f
      lift/stash-defaults
      (lift/replace-explainer explainer)))


;;; Useful predefined predicates

(defn must-exist [val]
  (not (nil? val)))

(defn member [& args]
  (letfn [(explainer [raw]
            (format "%s should be a member of %s; it is `%s`",
                    (path/friendly-path raw)
                    (pr-str args)
                    (pr-str (:leaf-value raw))))]
    (->> #(some (set args) %)
         (explain-with explainer))))
