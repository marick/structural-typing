(ns structural-typing.guts.explanations
  "All the non-custom error messages, together with functions
   and oopsies that contain them."
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.exval :as exval]
            [structural-typing.guts.expred :as expred]
            [structural-typing.assist.oopsie :as oopsie]))

(defn- structural-oopsie
  [original-path whole-value message]
  (merge (expred/->ExPred 'check-for-bad-structure
                          "bad structure detected"
                          (constantly message))
         (exval/->ExVal :halted-before-leaf-value-found
                        whole-value
                        original-path)))

(defn pluralize
  "It's convenient for callers to get back a singleton list of oopsies, rather
   than the single oopsie the `maker` returns."
  [maker]
  (fn [& args] (vector (apply maker args))))

;;; ---

(defn err:only-wrong-count
  "The error message produced by `ONLY` when a collection does not have only one element."
  [original-path collection-with-bad-arity]
  (cl-format nil "`~S` should be a path through a single-element collection; it passes through `~S`"
             original-path collection-with-bad-arity))
(defn oopsie:only-wrong-count [original-path whole-value collection-with-bad-arity]
  (structural-oopsie original-path whole-value
                     (err:only-wrong-count original-path collection-with-bad-arity)))
(def as-oopsies:only-wrong-count (pluralize oopsie:only-wrong-count))

;;; ---

(defn err:some-wrong-count
  "The error message produced by `SOME` when a collection does not have at least one element"
  [original-path empty-collection]
  (cl-format nil "`~S` should be a path to a non-empty collection; it ends in `~S`"
             original-path empty-collection))
(defn oopsie:some-wrong-count [original-path whole-value empty-collection]
  (structural-oopsie original-path whole-value
                     (err:some-wrong-count original-path empty-collection)))
(def as-oopsies:some-wrong-count (pluralize oopsie:some-wrong-count))

;;; ---

;; TODO: Condense
(defn err:bad-range-target
  "The error message produed when `RANGE` is applied to a non-sequential value"
  [original-path whole-value target]
  (cl-format nil "~A is not a path into `~S`; RANGE cannot make sense of non-collection `~S`"
             original-path whole-value target))
(defn oopsie:bad-range-target [original-path whole-value target]
  (structural-oopsie original-path whole-value
                     (err:bad-range-target original-path whole-value target)))
(def as-oopsies:bad-range-target (pluralize oopsie:bad-range-target))

(defn err:bad-all-target
  "The error message produed when `ALL` is applied to a non-collection or a map"
  [original-path whole-value target]
  (let [tag (if (map? target) "map" "non-collection")]
    (cl-format nil "~A is not a path into `~S`; ALL cannot make sense of ~A `~S`"
               original-path whole-value tag target)))
(defn oopsie:bad-all-target [original-path whole-value target]
  (structural-oopsie original-path whole-value
                     (err:bad-all-target original-path whole-value target)))
(def as-oopsies:bad-all-target (pluralize oopsie:bad-all-target))

;;; ---

(defn err:notpath
  "Produces the same error messsage produced when the whole value is not the same \"shape\" as the path requires."
  [original-path whole-value]
  (cl-format nil "~A is not a path into `~S`"
             (oopsie/friendly-path {:path original-path})
             whole-value))

(defn oopsie:notpath [original-path whole-value]
  (structural-oopsie original-path whole-value (err:notpath original-path whole-value)))
(def as-oopsies:notpath (pluralize oopsie:notpath))
