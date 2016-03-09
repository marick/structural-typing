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
  (letfn [(safe-juxt [args]
            (if (empty? args)
              (constantly [])
              (apply juxt args)))]
    (fn [kvs]
      (let [explainer (fn [oopsie]
                        (apply boa-explainer ((safe-juxt explainer-keys) oopsie)))]
        (vector (structural-oopsie (assoc kvs :explainer explainer)))))))


(defn mkfn:shouldbe-type [description]
  (fn [path bad-nonterminal]
    (format "%s encountered %s when %s was expected"
            (format/friendly-path path)
            (format/leaf bad-nonterminal) description)))

(def err:not-maplike (mkfn:shouldbe-type "a map or record"))
(def oopsies:not-maplike
  (mkfn:structural-singleton-oopsies err:not-maplike [:path :leaf-value]))

(def err:not-collection (mkfn:shouldbe-type "a collection"))
(def oopsies:not-collection
  (mkfn:structural-singleton-oopsies err:not-collection [:path :leaf-value]))

(def err:not-sequential (mkfn:shouldbe-type "a sequential collection"))
(def oopsies:not-sequential
  (mkfn:structural-singleton-oopsies err:not-sequential [:path :leaf-value]))

(defn err:maplike [path bad-nonterminal]
  (cl-format nil "~S encountered map or record `~S`; ALL doesn't allow that"
             path bad-nonterminal))
(def oopsies:maplike
  (mkfn:structural-singleton-oopsies err:maplike [:path :leaf-value]))


(defn err:selector-at-nil [path]
  (format (if (and (coll? path)
                   (> (count path) 1))
            "%s finds a `nil` at the position of the last component"
            "%s should not descend into `nil`")
          (format/friendly-path path)))
(def oopsies:selector-at-nil
  (mkfn:structural-singleton-oopsies err:selector-at-nil [:path]))

(defn err:whole-value-nil []
  "The whole value should not be `nil`")
(def oopsies:whole-value-nil
  (mkfn:structural-singleton-oopsies err:whole-value-nil []))

(defn err:value-nil [path]
  (format "%s has a `nil` value" (format/friendly-path path)))
(def oopsies:value-nil
  (mkfn:structural-singleton-oopsies err:value-nil [:path]))

(defn err:missing [path]
  (format "%s does not exist" (format/friendly-path path)))
(def oopsies:missing
  (mkfn:structural-singleton-oopsies err:missing [:path]))
