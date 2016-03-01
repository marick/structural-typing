(ns structural-typing.guts.explanations
  "All the non-custom error messages, together with functions
   and oopsies that contain them."
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.exval :as exval]
            [structural-typing.guts.expred :as expred]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.assist.format :as format]))

(defn structural-oopsie [kvs]
  (merge (expred/->ExPred 'check-for-bad-structure
                          "bad structure detected"
                          (constantly "no explainer"))
         (exval/->ExVal :halted-before-leaf-value-found
                        :no-whole-value
                        :no-path)
         kvs))

(defn mkfn:structural-singleton-oopsies [boa-explainer explainer-keys]
  (fn [kvs]
    (let [explainer (fn [oopsie]
                      (apply boa-explainer ((apply juxt explainer-keys) oopsie)))]
      (vector (structural-oopsie (assoc kvs :explainer explainer))))))


(defn mkfn:shouldbe-type [description]
  (fn [path bad-nonterminal]
    (format "%s encountered %s when %s was expected"
            (format/friendly-path path)
            (format/leaf bad-nonterminal) description)))

(def err:shouldbe-maplike (mkfn:shouldbe-type "a map or record"))
(def oopsies:shouldbe-maplike
  (mkfn:structural-singleton-oopsies err:shouldbe-maplike [:path :leaf-value]))

(def err:shouldbe-collection (mkfn:shouldbe-type "a collection"))
(def oopsies:shouldbe-collection
  (mkfn:structural-singleton-oopsies err:shouldbe-collection [:path :leaf-value]))

(def err:shouldbe-sequential (mkfn:shouldbe-type "a sequential"))
(def oopsies:shouldbe-sequential
  (mkfn:structural-singleton-oopsies err:shouldbe-sequential [:path :leaf-value]))

(defn err:shouldbe-not-maplike [path bad-nonterminal]
  (cl-format nil "~S encountered map or record `~S`; `ALL` doesn't allow that."
             path bad-nonterminal))
(def oopsies:shouldbe-not-maplike
  (mkfn:structural-singleton-oopsies err:shouldbe-not-maplike [:path :leaf-value]))


(defn err:should-not-be-applied-to-nil [path]
  (format (if (and (coll? path)
                   (> 1 (count path)))
            "%s applies the last component to `nil`"
            "%s should not descend into `nil`")
          (format/friendly-path path)))

(def oopsies:should-not-be-applied-to-nil
  (mkfn:structural-singleton-oopsies err:should-not-be-applied-to-nil [:path]))

(defn err:shouldbe-not-nil [path]
  (format "%s has a `nil` value" (format/friendly-path path)))
(def oopsies:shouldbe-not-nil
  (mkfn:structural-singleton-oopsies err:shouldbe-not-nil [:path]))

(defn err:shouldbe-present [path]
  (format "%s does not exist" (format/friendly-path path)))
(def oopsies:shouldbe-present
  (mkfn:structural-singleton-oopsies err:shouldbe-present [:path]))










(defn- structural-oopsie-old
  [original-path whole-value message]
  (merge (expred/->ExPred 'check-for-bad-structure
                          "bad structure detected"
                          (constantly message))
         (exval/->ExVal :halted-before-leaf-value-found
                        whole-value
                        original-path)))


(defn pluralize-old
  "It's convenient for callers to get back a singleton list of oopsies, rather
   than the single oopsie the `maker` returns."
  [maker]
  (fn [& args] (vector (apply maker args))))



;;; ---

;; TODO: Condense
(defn err:bad-range-target
  "The error message produed when `RANGE` is applied to a non-sequential value"
  [original-path whole-value target]
  (cl-format nil "~A is not a path into `~S`; RANGE cannot make sense of non-collection `~S`"
             original-path whole-value target))
(defn oopsie:bad-range-target [original-path whole-value target]
  (structural-oopsie-old original-path whole-value
                     (err:bad-range-target original-path whole-value target)))
(def as-oopsies:bad-range-target (pluralize-old oopsie:bad-range-target))

(defn err:bad-all-target
  "The error message produed when `ALL` is applied to a non-collection or a map"
  [original-path whole-value target]
  (let [tag (if (map? target) "map" "non-collection")]
    (cl-format nil "~A is not a path into `~S`; ALL cannot make sense of ~A `~S`"
               original-path whole-value tag target)))
(defn oopsie:bad-all-target [original-path whole-value target]
  (structural-oopsie-old original-path whole-value
                     (err:bad-all-target original-path whole-value target)))
(def as-oopsies:bad-all-target (pluralize-old oopsie:bad-all-target))

;;; ---

(defn err:notpath
  "Produces the same error messsage produced when the whole value is not the same \"shape\" as the path requires."
  [original-path whole-value]
  (cl-format nil "~A is not a path into `~S`"
             (oopsie/friendly-path {:path original-path})
             whole-value))

(defn oopsie:notpath [original-path whole-value]
  (structural-oopsie-old original-path whole-value (err:notpath original-path whole-value)))
(def as-oopsies:notpath (pluralize-old oopsie:notpath))
