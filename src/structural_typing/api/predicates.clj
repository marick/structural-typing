(ns structural-typing.api.predicates
  (:require [structural-typing.mechanics.m-lifting-predicates :as lift]
            [structural-typing.frob :as frob]
            [structural-typing.api.defaults :as default]))

;; Utilities

(defn show-as [name f]
  (when (fn? name) (frob/boom "First arg is a function. You probably got your args reversed."))
  (when-not (string? name) (frob/boom "First arg must be a string: %s %s" name f))
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
  
;; `required-key` is a special case. It is the only non-optional predicate. That is,
;; it - and it alone - doesn't ignore a `nil` value and return true. 
(def required-key
  "False iff a key/path does not exist or has value `nil`."
  (-> (compose-predicate (comp not nil?)
                         #(format "%s must exist and be non-nil" (default/friendly-path %)))
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
            (default/friendly-path %)
            (pr-str args)
            (pr-str (:leaf-value %)))))

