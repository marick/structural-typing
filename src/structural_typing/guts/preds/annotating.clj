(ns ^:no-doc structural-typing.guts.preds.annotating
  "Good reporting depends on lifted predicates being annotated with useful information."
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.preds.wrap :as wrap]))

(defn show-as 
  "Associate the given `name` string with the predicate for use when predicate failures
   are explained.
     
         (show-as \"less than 3\" (partial >= 3))
"
  [name predicate]
  (when (fn? name) (boom! "First arg is a function. You probably got your args reversed."))
  (when-not (string? name) (boom! "First arg must be a string: %s %s" name predicate))
  (-> predicate
      wrap/stash-defaults
      (wrap/replace-predicate-string name)))

(defn explain-with
  "After the `predicate` fails, the failure will need to be explained. Arrange for
   `explainer` to be called with the [[oopsie]] that results from the
   failure.
   
        (explain-with #(format \"Yo! %s has %s characters, which is WAY too long.\"
                               (:leaf-value %)
                               (count (:leaf-value %)))
                      #(< (count %) 8)))
"
  [explainer predicate]
  (-> predicate
      wrap/stash-defaults
      (wrap/replace-explainer explainer)))




