(ns structural-typing.pred-writing.lifting
  "An interface to the main mechanisms by which parts of a type are assembled
   and used. For advanced use: writing complicated purposes, checking without
   the use of a type repo (as in Midje). A WORK IN PROGRESS.
"
  (:require [structural-typing.pred-writing.shapes.oopsie :as oopsie]
            [structural-typing.pred-writing.shapes.expred :as expred]
            [structural-typing.guts.mechanics.canonicalizing-types :as canon]
            [structural-typing.guts.shapes.pred :as pred]
            [such.function-makers :as mkfn])
  (:use such.shorthand))

(defn lift-expred
  "Like [[lift]], except that (1) using an already-lifted predicate is
   an error, and (2) the argument is an [[ExPred]] instead of an actual
   predicate."
  [expred & protection-subtractions]
  (pred/lift-expred expred protection-subtractions))

(defn lift
  "Convert a predicate into a function that produces either an empty
   sequence or a singleton vector containing an [[oopsie]] describing
   the failure of the predicate. However, nothing is done to an 
   already-lifted predicate.

   Normally, the predicate is wrapped so that (1) a `nil` value is considered
   `true`, and (2) an exception during evaluation is considered `false`. These
   can be omitted with the `:check-nil` and `:allow-exceptions` protection-subtractions.

   The lifted predicate takes an [[exval]] as its argument.
"
  [pred & protection-subtractions]
  (if (pred/already-lifted? pred)
    pred
    (apply lift-expred (pred/->expred pred) protection-subtractions)))

(defn nested-type->lifted-pred [type-signifiers]
  )
