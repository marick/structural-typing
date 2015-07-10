(ns structural-typing.api.predicates
  "Functions used to construct predicates that explain themselves, plus some
   frequently useful predicates of that sort.

   Much of this is gathered into the catchall `structural-typing.types` namespace."
  (:require [structural-typing.mechanics.lifting-predicates :as lift]
            [structural-typing.frob :as frob]
            [structural-typing.api.custom :as custom]))

;; Utilities

(defn show-as 
  "Associate the given `name` string with the predicate for use when predicate failures
   are explained.
     
         (->> (partial >= 3) (show-as \"less than 3\"))
"
  [name predicate]
  (when (fn? name) (frob/boom! "First arg is a function. You probably got your args reversed."))
  (when-not (string? name) (frob/boom! "First arg must be a string: %s %s" name predicate))
  (-> predicate
      lift/stash-defaults
      (lift/replace-predicate-string name)))

(defn explain-with
  "After the `predicate` fails, the failure will need to be explained. Arrange for
   the `explainer` function to be called with the [[oopsie]] that results from the
   failure."
  [explainer predicate]
  (-> predicate
      lift/stash-defaults
      (lift/replace-explainer explainer)))

(defn should-be [format-string expected]
  #(format format-string,
           (custom/friendly-path %)
           (pr-str expected)
           (pr-str (:leaf-value %))))


(defn- compose-predicate [name pred fmt-fn]
  (->> pred
       (show-as name)
       (explain-with fmt-fn)))


;;; Predefined predicates
  
;; `required-key` is a special case. It is the only non-optional predicate. That is,
;; it - and it alone - doesn't ignore a `nil` value and return true. 
(def required-key
  "False iff a key/path does not exist or has value `nil`. This is the only
   predicate that is not considered optional."
  (-> (compose-predicate "required-key"
                         (comp not nil?)
                         #(format "%s must exist and be non-nil" (custom/friendly-path %)))
      (lift/lift* false)))


(defn member
  "Produce a predicate that's false when applied to a value not a member of `coll`. The explainer
   associated with `member` prints those `colls`.
     
         ( (member [2 3 5 7]) 4) => false
         (type! :small-primes {:n (member [2 3 5 7])})
"
  [coll]
  (compose-predicate
   (format "(member %s)" coll)
   #(boolean ((set coll) %))
   (should-be "%s should be a member of `%s`; it is `%s`" coll)))

(defn exactly
  "Produce a predicate that's true iff the value it's applied to is `=` to `x`.
    
        ( (exactly 5) 4) => false
        (type! :V5 {:version (exactly 5)})
"
  [x]
  (compose-predicate
   (format "(exactly %s)" x)
   (partial = x)
   (should-be "%s should be exactly `%s`; it is `%s`" x)))
   


