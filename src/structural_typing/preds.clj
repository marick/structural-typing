(ns structural-typing.preds
  "All of the predefined predicates."
  (:require [structural-typing.guts.preds.annotated :as annotated :refer [show-as explain-with]]
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
  (mechanics/lift-expred {:explainer #(format "%s must exist and be non-nil"
                                                          (oopsie/friendly-path %))
                            :predicate-string "required-key"
                            :predicate #(not (nil? %))}
                           :check-nil))

(defn implies
  "Each `if-pred` is evaluated in turn. When the `if-pred` is
   truthy, the corresponding `then-pred` is evaluated. Checking
   will produce all of the errors from all of the `then-preds`
   that were tried.
   
       (type! :Sep {:a (pred/implies neg? even?
                                     neg? (show-as \"=3\" (partial = 3))
                                     string? empty?)})

       user=> (checked :Sep {:a 1}) ; Neither `neg?` nor `string?`
       => {:a 1}
       
       user=> (checked :Sep {:a -1}) ; Two clauses check
       :a should be `=3`; it is `-1`
       :a should be `even?`; it is `-1`
       => nil
       
       user=> (checked :Sep {:a \"long\"}) ; Final clause checks
       :a should be `empty?`; it is `\"long\"`
       => nil
   
   Note that, unlike most predicates in this namespace, 
   `implies` cannot be used as an ordinary predicate. It
   doesn't return a truthy/falsey value but rather a sequence
   of [[oopsies]].
"
  {:arglists '([if-pred then-pred...])}
  [& args]
  (let [implications (->> args
                          (map mechanics/lift)
                          (partition 2))
        f (fn [call-info]
            (reduce (fn [so-far [antecedent consequent]]
                      (if (empty? (antecedent call-info))
                        (into so-far (consequent call-info))
                        so-far))
                    []
                    implications))]
    (->> f annotated/mark-as-lifted (show-as "implies"))))
