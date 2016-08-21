(ns ^:no-doc structural-typing.guts.preds.pseudopreds
  "Preds that are used througout"
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.preds.wrap :as wrap]
            [structural-typing.guts.expred :as expred]
            [structural-typing.guts.explanations :as explain]
            [structural-typing.assist.oopsie :as oopsie]
            [such.metadata :as meta]
            [defprecated.core :as depr])
  (:refer-clojure :exclude [any?]))

(defn rejects-missing-and-nil [pred]
  (meta/assoc pred ::special-case-handling {:reject-missing? true, :reject-nil? true}))
(defn rejects-missing [pred]
  (meta/assoc pred ::special-case-handling {:reject-missing? true, :reject-nil? false}))
(defn rejects-nil [pred]
  (meta/assoc pred ::special-case-handling {:reject-missing? false, :reject-nil? true}))

(defn special-case-handling [pred]
  (meta/get pred ::special-case-handling {}))

(defn rejects-missing-and-nil? [x]
  (= (special-case-handling x) {:reject-missing? true, :reject-nil? true}))

(def reject-nil
  "False iff the value given is `nil`. By default, type descriptions allow nil values,
  following Clojure's lead. To reject nils, use type descriptions like this:

      (type! :X {:a [reject-nil string?]})

   ... or, when checking types directly:

      (built-like [string? reject-nil] nil)

   See also [[reject-missing]] and [[required-path]].
"
  (-> (expred/->ExPred (comp not nil?)
                       "reject-nil"
                       #(explain/err:selector-at-nil (oopsie/friendly-path %)))
      (wrap/lift-expred [:check-nil])
      rejects-nil))

(def required-path
  "False iff a key/path does not exist or has value `nil`. 
  See also [[reject-missing]] and [[reject-nil]].
"
  ;; When called directly, it can't get a 'missing' value, so it's the same
  ;; as `reject-nil`.
  (-> (expred/->ExPred (comp not nil?)
                       "required-path"
                       #(explain/err:selector-at-nil (oopsie/friendly-path %)))
      (wrap/lift-expred [:check-nil])
      rejects-missing-and-nil))

(def reject-missing
  "This appears in a predicate list, but it is never called directly. Its appearance
  means that cases like the following are rejected:

      user=> (type! :X {:a [string? reject-missing]})
      user=> (built-like :X {})
      :a does not exist

      user=>  (type! :X {[(RANGE 0 3)] [reject-missing even?]})
      user=> (built-like :X [])
      [0] does not exist
      [1] does not exist
      [2] does not exist

   See also [[reject-nil]] and [[required-path]].
"
  (-> (expred/->ExPred (constantly true)
                       "reject-missing"
                       #(boom! "reject-missing should never fail: %s"))
      (wrap/lift-expred [])
      rejects-missing))


(defn max-rejection [preds]
  (let [raw-data (map special-case-handling preds)]
    {:reject-nil? (any? true? (map :reject-nil? raw-data))
     :reject-missing? (any? true? (map :reject-missing? raw-data))}))

(defn without-pseudopreds [preds]
  (remove #(let [rejectionism (special-case-handling %)]
             (or (:reject-nil? rejectionism)
                 (:reject-missing? rejectionism)))
          preds))


(def not-nil-fn
  "False iff a key/path does not exist or has value `nil`.

   Note: At some point in the future, this library might make a distinction
   between a `nil` value and a missing key. If so, this predicate will change
   to reject `nil` values but be silent about missing keys. See [[required-path]].
"
  (-> (expred/->ExPred (comp not nil?)
                       "not-nil"
                       #(format "%s is nil, and that makes Sir Tony Hoare sad"
                                (oopsie/friendly-path %)))
      (wrap/lift-expred [:check-nil])
      rejects-nil))

(depr/defn not-nil
  "Deprecated in favor of `required-path`, `reject-nil`, or `reject-missing`."
  [& args]
  {:deprecated {:in "2.0.0"
                :use-instead reject-nil}}
  (apply not-nil-fn args))
