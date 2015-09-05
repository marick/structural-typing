(ns structural-typing.preds
  "Predefined predicates that are not imported into `structural-typing.type`."
  (:use structural-typing.clojure.core)
  (:require [structural-typing.assist.lifting :as lifting]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.guts.preds.wrap :as wrap]
            [structural-typing.assist.expred :as expred]
            [structural-typing.guts.type-descriptions.substituting :as subst]
            [such.readable :as readable]))

(defn- should-be [format-string expected]
  #(format format-string,
           (oopsie/friendly-path %)
           (readable/value-string expected)
           (readable/value-string (:leaf-value %))))

(defn- compose-predicate [name pred fmt-fn]
  (->> pred
       (wrap/show-as name)
       (wrap/explain-with fmt-fn)))

;;;                      THE ACTUAL PREDICATES

(defn member
  "Produce a predicate that's false when applied to a value not a
   member of `coll`. The explainer associated with `member` prints
   those `colls`.
     
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
     (cond (every? regex? [actual expected])
           (= (str actual) (str expected))
           
           (regex? expected)
           (boolean (re-find expected actual))

           :else
           (= actual expected)))
   (should-be "%s should match `%s`; it is `%s`" expected)))


;;; More exotic predicate creation.


;; Too many places accept vectors or single arguments. This is an experiment
;; to disallow vectors in favor of a function (rather than having, as elsewhere,
;; a function that's just an alias for `vector`).

(defrecord AllOf [type-descriptions])
(alter-meta! #'->AllOf assoc :private true)
(alter-meta! #'map->AllOf assoc :private true)

(defn all-of
  "This is used with [[implies]] to group a collection of `type-descriptions`
   into one. Unlike [[type!]] and [[named]], `all-of` doesn't support a final
   \"& rest\" argument."
  [& type-descriptions]
  (->AllOf type-descriptions))
(defn ^:no-doc force-all-of [x]
  (if (instance? AllOf x)
    x
    (all-of x)))

(defn ^:no-doc implies:mkfn:from-adjusted [adjusted-pairs]
  (->> (fn [exval]
         (letfn [(adjust-path [oopsie]
                   (update oopsie :path #(into (:path exval) %)))]
           (reduce (fn [so-far [antecedent consequent]]
                     (if (antecedent (:leaf-value exval))
                       (into so-far (map adjust-path (consequent (:leaf-value exval))))
                       so-far))
                   []
                   adjusted-pairs)))
       wrap/mark-as-lifted
       (wrap/show-as "implies")))

(defn implies
  "Each `if-pred` is evaluated in turn. When the `if-pred` is
   truthy, the corresponding `then-part` is applied. The `then-part`
   is either a single condensed type description or a set of them
   enclosed in [[all-of]].

   Perhaps the most common use of `implies` is to say \"if one key
   exists in a map, another must also exist\":

       user=> (type! :X (pred/implies :a :b))

       user=> (checked :X {:a 1, :b 2})
       => {:a 1, :b 2}
       user=> (checked :X {:a 1})
       :b must exist and be non-nil
       => nil

   Note that the `then-part` is irrelevant if `:a` is not present:

       user=> (checked :X {})
       => {}
       user=> (checked :X {:b 1})
       => {:b 1}
   
   The `then-part` can be any *single* condensed type description. For
   example, the following requires three keys to exist if `:a` does.

       (type! :X (pred/implies :a [:b :c :d]))

   Here, if `:a` is even, `:b` must be present and a `:Point`:

       (type! :X (pred/implies (comp even? :a)
                               {:b [required-key (includes :Point)]}))

   There can be more than one if/then pair:   
   
       (type! :Sep {:a (pred/implies neg? even?
                                     neg? (show-as \"=3\" (partial = 3))
                                     string? empty?)})

       user=> (checked :Sep {:a 1}) ; Neither `neg?` nor `string?`
       => {:a 1}
       
       user=> (checked :Sep {:a -1}) ; Two checked, two fail.
       :a should be `=3`; it is `-1`
       :a should be `even?`; it is `-1`
       => nil
       
       user=> (checked :Sep {:a \"long\"}) ; String check fails
       :a should be `empty?`; it is `\"long\"`
       => nil
   
   Note that, unlike most predicates in this namespace, 
   `implies` cannot be used as an ordinary predicate. It
   doesn't return a truthy/falsey value but rather a function that
   returns a function.
"
  {:arglists '([if-pred condensed-type-description if-pred condensed-type-description...])}
  [& args]

  (-> (fn [type-map]
        (let [make-antecedent pred:exception->false
              make-consequent #(-> %
                                   force-all-of
                                   :type-descriptions
                                   (lifting/lift-type-descriptions type-map))
              adjusted-pairs (->> args
                                  (alternately make-antecedent make-consequent)
                            (partition 2))]
          (implies:mkfn:from-adjusted adjusted-pairs)))
      subst/as-type-expander))
