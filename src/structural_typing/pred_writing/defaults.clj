(ns structural-typing.pred-writing.defaults
  "User-visible default behaviors.

   Much of this is gathered into the catchall `structural-typing.types` namespace."
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :as str]
            [such.readable :as readable])
  (:require [structural-typing.pred-writing.oopsie :as oopsie]))

(readable/set-function-elaborations! {:anonymous-name "<custom-predicate>" :surroundings ""})

(defn default-predicate-explainer
  "Converts an [[oopsie]] into a string of the form \"%s should be %s; it is %s\"."
  [{:keys [predicate-string leaf-value] :as oopsie}]
  (format "%s should be `%s`; it is `%s`"
          (oopsie/friendly-path oopsie)
          predicate-string
          (pr-str leaf-value)))

(def default-success-handler 
  "The default success handler just returns the original candidate structure passed to `checked`."
  identity)

(def default-error-handler
  "This error handler takes the output of type checking (a sequence of [[oopsies]]) and prints
   each one's explanation to standard output. It returns
   `nil`, allowing constructs like this:
   
        (some-> (type/checked :frobnoz x)
                (assoc :goodness true)
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
  (throw (new Exception (str/join "\n" (oopsie/explanations oopsies)))))
