(ns ^:no-doc structural-typing.mechanics.m-preds
  (:require [structural-typing.mechanics.lifting-predicates :as lift]
            [structural-typing.mechanics.frob :as frob]
            [structural-typing.api.oopsie :as oopsie]))


(defn show-as 
  "Associate the given `name` string with the predicate for use when predicate failures
   are explained.
     
         (->> (partial >= 3) (show-as \"less than 3\"))
"
  [name predicate]
  (when (fn? name) (frob/boom! "First arg is a function. You probably got your args reversed."))
  (when-not (string? name) (frob/boom! "First arg must be a string: %s %s" name predicate))
  (-> predicate
      lift/stash-defaults
      (lift/replace-predicate-string name)))

(defn explain-with
  "After the `predicate` fails, the failure will need to be explained. Arrange for
   the `explainer` function to be called with the [[oopsie]] that results from the
   failure."
  [explainer predicate]
  (-> predicate
      lift/stash-defaults
      (lift/replace-explainer explainer)))

(defn compose-predicate [name pred fmt-fn]
  (->> pred
       (show-as name)
       (explain-with fmt-fn)))

(def required-key
  "False iff a key/path does not exist or has value `nil`. This is the only
   predicate that is not considered optional."

  (-> (compose-predicate "required-key"
                         (comp not nil?)
                         #(format "%s must exist and be non-nil" (oopsie/friendly-path %)))
      (lift/lift* false)))


