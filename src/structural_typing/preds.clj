(ns structural-typing.preds
  "All of the predefined predicates."
  (:require [structural-typing.mechanics.m-preds :as pred]
            [structural-typing.mechanics.frob :as frob]
            [structural-typing.api.custom :as custom]
            [such.immigration :as ns]))

;; Add required-key for completeness (also in types.clj).
(ns/import-vars [structural-typing.mechanics.m-preds required-key])

(defn- should-be [format-string expected]
  #(format format-string,
           (custom/friendly-path %)
           (pr-str expected)
           (pr-str (:leaf-value %))))

(defn member
  "Produce a predicate that's false when applied to a value not a member of `coll`. The explainer
   associated with `member` prints those `colls`.
     
         ( (member [2 3 5 7]) 4) => false
         (type! :small-primes {:n (member [2 3 5 7])})
"
  [coll]
  (pred/compose-predicate
   (format "(member %s)" coll)
   #(boolean ((set coll) %))
   (should-be "%s should be a member of `%s`; it is `%s`" coll)))

(defn exactly
  "Produce a predicate that's true iff the value it's applied to is `=` to `x`.
    
        ( (exactly 5) 4) => false
        (type! :V5 {:version (exactly 5)})
"
  [x]
  (pred/compose-predicate
   (format "(exactly %s)" x)
   (partial = x)
   (should-be "%s should be exactly `%s`; it is `%s`" x)))
