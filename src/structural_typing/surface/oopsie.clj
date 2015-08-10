(ns structural-typing.surface.oopsie
  "The declaration of the core data structure: the oopsie. It is produced when a
   predicate is applied to a value and fails. Also utility functions."
  (:require [structural-typing.guts.paths.readable :as readable-path]))

;; TODO: should make this a record.

(defrecord ExVal [path whole-value leaf-value])
(defrecord ExPred [predicate predicate-string predicate-explainer])

(def oopsie
   "An oopsie contains these fields, all potentially relevant when dealing with
   the failure of a predicate.

   * **whole-value**: the original value passed to [[checked]].
   * **leaf-value**: the value passed to the predicate.
   * **path**: A [Specter](https://github.com/nathanmarz/specter)-style path.
   * **predicate**: the original predicate (any callable)
   * **predicate-string**: a friendly string, such as `even?` instead
     of `#<core$even_QMARK_ clojure.core$even_QMARK_@47a01b6e>`
   * **predicate-explainer**: The explainer function associated with the
     predicate. It is applied to the oopsie. It usually produces a string, 
     but it could produce anything that your custom failure handler handles.

   This var doesn't actually do anything. It's just here as the endpoint for 
   links in docstrings."
nil)

(def oopsies
  "See above."
  nil)

(defn ->oopsie [& recs]
  (apply merge recs))

(defn friendly-path
  "Convert the oopsie's path into a string, with predicates and function components printed nicely."
  [oopsie]
  (->> oopsie
       :path
       readable-path/friendly))


(defn explanation
  "Convert an [[oopsie]] into a string explaining the error,
   using the `:predicate-explainer` within it."
  [oopsie]
  ((:predicate-explainer oopsie) oopsie))

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
