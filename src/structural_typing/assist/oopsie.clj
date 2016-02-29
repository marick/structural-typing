(ns structural-typing.assist.oopsie
  "The declaration of the core data structure: the oopsie. It is produced when a
   predicate is applied to a value and fails. Also utility functions."
  (:use structural-typing.clojure.core)
  (:require [structural-typing.assist.format :as format]))


(def oopsie
   "An oopsie contains these fields, all potentially relevant when dealing with
   the failure of a predicate.

   * **whole-value**: the original value passed to [[built-like]].
   * **leaf-value**: the value passed to the predicate.
   * **path**: The specific path that lead from the whole value to the leaf value.
     It will not contain tokens like `ALL`; rather; it will contain specific indexes.
   * **predicate**: The predicate (any callable) that failed. Note: this is the original,
     not the version from [[lift-pred]].
   * **predicate-string**: a friendly string, such as `even?` instead
     of `#<core$even_QMARK_ clojure.core$even_QMARK_@47a01b6e>`
   * **explainer**: A function that explains this particular oopsie, usually
     naming the original predicate, path, and leaf value. It usually produces a string, 
     but it could produce anything that your custom failure handler handles.

   This var doesn't actually do anything. It's just here as the endpoint for 
   links in docstrings."
nil)

(def oopsies
  "See above."
  nil)

(defn friendly-path
  "Convert the oopsie's path into a string, with predicates and function components printed nicely."
  [oopsie]
  (format/friendly-path (:path oopsie)))

(defn explanation
  "Convert an [[oopsie]] into a string explaining the error,
   using the `:explainer` within it."
  [oopsie]
  (if-let [explainer (:explainer oopsie)]
    (explainer oopsie)
    (boom! "Oopsie `%s` has no explainer function" oopsie)))

(defn explanations 
  "Convert a collection of [[oopsies]] into a lazy sequence of explanatory strings.
   See [[explanation]]. The results are sorted."
  [oopsies]
  (sort (map explanation oopsies)))

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
