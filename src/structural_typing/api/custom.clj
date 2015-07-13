(ns structural-typing.api.custom
  "Functions useful when overriding default behavior."
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :as str]
            [clojure.repl :as repl])
  (:require [structural-typing.api.path :as path]
            [such.readable :as readable]))

(readable/instead-of path/ALL 'ALL)
(readable/set-function-elaborations! {:anonymous-name "your custom predicate" :surroundings ""})

(defn friendly-function-name 
  "The argument should be a function or multimethod. Produce a string that will help a
   human understand which chunk o' code is being referred to.

       (d/friendly-function-name even?) => \"even?\"
       (d/friendly-function-name #'even?) => \"even?\"
"
  [f]
  (readable/fn-string f))


(defn- path-string [components]
  (if (= 1 (count components))
    (str (first components))
    (cl-format nil "[~{~A~^ ~}]" components)))

(defn friendly-path
  "Convert the path into a string, with Specter components printed nicely."
  [oopsie]
  (->> oopsie
       :path
       readable/value
       path-string))

(defn explanation
  "Convert an [[oopsie]] into a string explaining the error,
   using the `:predicate-explainer` within it."
  [oopsie]
  ((:predicate-explainer oopsie) oopsie))

(defn explanations 
  "Convert a collection of [[oopsies]] into a lazy sequence of explanatory strings.
   See [[explanation]]."
  [oopsies]
  (map explanation oopsies))

(defn mkfn:apply-to-each-explanation
  "Checking a single candidate may result in multiple errors ([[oopsies]]). 
   The generated function applies the `handler` to each oopsie's [[explanation]] in
   turn. The `handler` must be called for side-effect, as the generated function
   always returns `nil`."
  [handler]
  (fn [oopsies]
    (doseq [e (explanations oopsies)]
      (handler e))
    nil))
  
(defn mkfn:apply-to-explanation-collection
  "Checking a single candidate may result in multiple errors ([[oopsies]]). 
   The generated function applies the `handler` once to a collection of all the oopsie's
   [[explanations]]. The value it returns is whatever the `handler` returns; it is
   not guaranteed to be `nil`."
  [handler]
  (fn [oopsies]
    (handler (explanations oopsies))))
