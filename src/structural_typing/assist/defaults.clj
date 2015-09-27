(ns structural-typing.assist.defaults
  "User-visible default behaviors.

   Much of this is gathered into the catchall `structural-typing.types` namespace."
  (:use structural-typing.clojure.core)  ; yes: `use`. Glorious, skimmable, terse `use`.
  (:require [such.readable :as readable])
  (:require [structural-typing.assist.oopsie :as oopsie]))

(readable/set-function-elaborations! {:anonymous-name "<custom-predicate>" :surroundings ""})

(import-vars [structural-typing.guts.expred default-predicate-explainer])

(def default-success-handler 
  "The default success handler just returns the original candidate structure passed to `built-like`."
  identity)

(def default-error-handler
  "This error handler takes the output of type checking (a sequence of [[oopsies]]) and prints
   each one's explanation to standard output. It returns
   `nil`, allowing constructs like this:
   
        (some-> (type/built-like :Patient p)
                (assoc :handled true)
                ...)
"
  (oopsie/mkfn:apply-to-each-explanation println))

(defn throwing-error-handler 
  "In contrast to the default error handler, this one throws a
   `java.lang.Exception` whose message is the concatenation of the
   [[explanations]] of the [[oopsies]].
   
   To make all type mismatches throw failures, do this:
   
          (global-type/on-error! type/throwing-failure-handler) ; for the global type repo
          (type/replace-error-handler type-repo type/throwing-failure-handler) ; local repo
"
  [oopsies]
  (throw (new Exception (str-join "\n" (oopsie/explanations oopsies)))))
