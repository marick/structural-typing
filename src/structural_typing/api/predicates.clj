(ns structural-typing.api.predicates
  (:require [structural-typing.mechanics.m-lifting-predicates :as lift]
            [structural-typing.api.path :as path]))

;; Utilities

(defn show-as [name f]
  (-> f
      lift/stash-defaults
      (lift/replace-predicate-string name)))

(defn explain-with [explainer f]
  (-> f
      lift/stash-defaults
      (lift/replace-explainer explainer)))

(defn compose-predicate [pred fmt-fn]
  (->> pred (explain-with fmt-fn)))


;;; Predefined predicates
  
;; `must-exist` is a special case. It is the only non-optional predicate. That is,
;; it - and it alone - doesn't ignore a `nil` value and return true. 
(def must-exist
  "False iff a key/path does not exist or has value `nil`."
  (-> (compose-predicate (comp not nil?)
                         #(format "%s must exist and be non-nil" (path/friendly-path %)))
      (lift/lift* false)))


(defn member
  "Produce a predicate that's false when applied to a value not a member of `args`.
   
        ( (member 2 3 5 7) 4) => false
        (type/named! :small-primes {:n (member 2 3 5 7)})
"
  [& args]
  (compose-predicate
   #(some (set args) %)
   #(format "%s should be a member of %s; it is `%s`",
            (path/friendly-path %)
            (pr-str args)
            (pr-str (:leaf-value %)))))

