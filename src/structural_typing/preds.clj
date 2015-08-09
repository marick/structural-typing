(ns structural-typing.preds
  "All of the predefined predicates."
  (:require [structural-typing.guts.preds.annotated :refer [show-as explain-with]]
            [structural-typing.surface.mechanics :as mechanics]
            [structural-typing.guts.frob :as frob]
            [structural-typing.surface.oopsie :as oopsie]
            [such.readable :as readable]
            [such.types :as types]
            [such.immigration :as ns]))

(defn- should-be [format-string expected]
  #(format format-string,
           (oopsie/friendly-path %)
           (pr-str expected)
           (pr-str (:leaf-value %))))

(defn- compose-predicate [name pred fmt-fn]
  (->> pred
       (show-as name)
       (explain-with fmt-fn)))



(defn member
  "Produce a predicate that's false when applied to a value not a member of `coll`. The explainer
   associated with `member` prints those `colls`.
     
         ( (member [2 3 5 7]) 4) => false
         (type! :small-primes {:n (member [2 3 5 7])})
"
  [coll]
  (compose-predicate
   (format "(member %s)" (readable/value-string coll))
   #(boolean ((set coll) %))
   (should-be "%s should be a member of `%s`; it is `%s`" coll)))

(defn exactly
  "Produce a predicate that's true iff the value it's applied to
   is `=` to `x`.
     
         ( (exactly 5) 4) => false
         (type! :V5 {:version (exactly 5)})
"
  [expected]
  (compose-predicate
   (format "(exactly %s)" (readable/value-string expected))
   (partial = expected)
   (should-be "%s should be exactly `%s`; it is `%s`" expected)))


(defn ^:no-doc matches
  "Doc"
  [expected]
  (compose-predicate
   (format "(matches %s)" (readable/value-string expected))
   (fn [actual]
     (cond (every? types/regex? [actual expected])
           (= (str actual) (str expected))
           
           (types/regex? expected)
           (boolean (re-find expected actual))

           :else
           (= actual expected)))
   (should-be "%s should match `%s`; it is `%s`" expected)))


;;; More exotic predicate creation.

(def required-key
  "False iff a key/path does not exist or has value `nil`. This is the only
   predefined predicate that is not considered optional."
  (mechanics/lift-pred-map {:predicate-explainer #(format "%s must exist and be non-nil"
                                                          (oopsie/friendly-path %))
                            :predicate-string "required-key"
                            :predicate #(not (nil? %))}))

