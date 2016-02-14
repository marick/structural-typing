(ns structural-typing.assist.defaults
  "User-visible default behaviors.

   Much of this is gathered into the catchall `structural-typing.types` namespace."
  (:use structural-typing.clojure.core)  ; yes: `use`. Glorious, skimmable, terse `use`.
  (:require [such.readable :as readable])
  (:require [structural-typing.assist.oopsie :as oopsie]))

(def anonymous-name "<custom-predicate>")
(readable/set-function-elaborations! {:anonymous-name anonymous-name :surroundings ""})


(defn function-as-bad-value-string [f]
  (let [s (readable/value-string f)]
    (if (= s anonymous-name)
      (pr-str f)
      s)))


(defn default-predicate-explainer
  "Converts an [[oopsie]] into a string of the form \"%s should be `%s`; it is `%s`\"."
  [{:keys [predicate-string leaf-value] :as expred}]
  (format "%s should be `%s`; it is %s"
          (oopsie/friendly-path expred)
          predicate-string
          (cond (extended-fn? leaf-value)
                (format "the function `%s`" (function-as-bad-value-string leaf-value))

                :else
                (str "`" (pr-str leaf-value) "`"))))

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
   
          (global-type/on-error! type/throwing-error-handler) ; for the global type repo
          (type/replace-error-handler type-repo type/throwing-error-handler) ; local repo
"
  [oopsies]
  (throw (new Exception (str-join "\n" (oopsie/explanations oopsies)))))
