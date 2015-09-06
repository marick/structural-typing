(ns structural-typing.assist.lifting
  "*Lifting* is a core concept. A predicate begins as something that takes a value and
   returns a truthy/falsey value. It is lifted into something that takes an [[exval]]
   (extended value) and returns a (possibly empty) sequence of [[oopsies]]. Condensed
   type descriptions are similarly converted into single functions that return oopsies.
"
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.type-descriptions :as type-descriptions]
            [structural-typing.guts.preds.wrap :as wrap]))

(defn lift-pred
  "Convert a predicate into a function that produces either an empty
   sequence or a vector containing an [[oopsie]] describing
   the failure of the predicate. However, nothing is done to an 
   already-lifted predicate.

   Normally, the predicate is wrapped so that (1) a `nil` value is considered
   `true`, and (2) an exception during evaluation is considered `false`. These
   can be omitted with the `:check-nil` and `:allow-exceptions` protection-subtractions.

   The lifted predicate takes an [[exval]] as its argument.
"
  [pred & protection-subtractions]
  (wrap/lift pred protection-subtractions))

(defn lift-type
  "Take a collection of condensed type descriptions. Canonicalize them.
   Convert the result into a function that returns [[oopsies]]."
  ([condensed-type-descriptions type-map]
     (type-descriptions/lift condensed-type-descriptions type-map))
  ([condensed-type-descriptions]
     (lift-type condensed-type-descriptions {})))
