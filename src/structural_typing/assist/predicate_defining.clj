(ns structural-typing.assist.predicate-defining
  "Helpers for defining custom predicates. See `structural-typing.preds` for examples of use."
  (:use structural-typing.clojure.core)
  (:require [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.assist.format :as format]
            [structural-typing.guts.preds.annotating :as annotating]
            [such.readable :as readable])
  (:refer-clojure :exclude [any?]))

(defn should-be
  "The typical explanation string is built from a path, expected value,
  and leaf value, in that order. The path and leaf value can be gotten from
  an [[oopsie]]. This, then, is shorthand that lets you build an explainer
  function from just a format string and an expected value.

      (should-be \"%s should be a member of `%s`; it is `%s`\" coll)
"
  [format-string expected]
  #(format format-string,
           (oopsie/friendly-path %)
           (readable/value-string expected)
           (readable/value-string (:leaf-value %))))

(defn compose-predicate
  "A checker can be any function. But it's often more useful to \"compose\" a checker
  predicate from three parts: its name to be printed (such as `\"(member [1 2 3])\"`),
  a function, and an explainer function that converts an [[oopsie]] into a string. This
  function creates a checker function from those three parts.

  Note that `expected` is formatted as a readable value. So, for example, strings appear
  in the explanation surrounded by quotes.

  The resulting predicate still returns the same values as the second argument (the
  \"underlying\" predicate), but it has extra metadata."
  [name pred fmt-fn]
  (->> pred
       (annotating/show-as name)
       (annotating/explain-with fmt-fn)))
