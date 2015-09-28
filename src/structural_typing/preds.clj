(ns structural-typing.preds
  "Predefined predicates that are not imported into `structural-typing.type`."
  (:use structural-typing.clojure.core)
  (:require [structural-typing.assist.lifting :as lifting]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.guts.type-descriptions.includes :as includes]
            [such.readable :as readable])
  (:use structural-typing.assist.special-words))

(defn- should-be [format-string expected]
  #(format format-string,
           (oopsie/friendly-path %)
           (readable/value-string expected)
           (readable/value-string (:leaf-value %))))

(defn- compose-predicate [name pred fmt-fn]
  (->> pred
       (show-as name)
       (explain-with fmt-fn)))

;;;                      THE ACTUAL PREDICATES

(defn member
  "Produce a predicate that's false when applied to a value not a
   member of `coll`. The explainer associated with `member` prints
   `coll`.
     
         (type! :small-primes {:n (member [2 3 5 7])})
"
  [coll]
  (compose-predicate
   (format "(member %s)" (readable/value-string coll))
   #(boolean ((set coll) %))
   (should-be "%s should be a member of `%s`; it is `%s`" coll)))

(defn exactly
  "Produce a predicate that's true iff the value it's applied to
   is `expected`.
     
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

(defrecord AllOf [condensed-type-descriptions])
(alter-meta! #'->AllOf assoc :private true)
(alter-meta! #'map->AllOf assoc :private true)

(defn all-of
  "This is used with [[implies]] to group a collection of `condensed-type-descriptions`
   into one. 
   
        (all-of (requires :x :y) (includes :Point) {:color string?})
"
  [& condensed-type-descriptions]
  (->AllOf condensed-type-descriptions))
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
       lifting/mark-as-lifted
       (show-as "implies")))

(defn implies
  "Each `if-pred` is evaluated in turn. When the `if-pred` is
   truthy, the corresponding `then-part` applies. The `then-part`
   is either a single condensed type description or a set of them
   enclosed in [[all-of]].
   
        (type! :X (pred/implies :a :b)) 
        (type! :X (pred/implies (comp nil :a) (requires :b :c :d)))
        (type! :X (pred/implies :a (pred/all-of (includes :Point)
                                                (requires :color))))

   There's enough going on with `implies` that it has its own
   page in the user documentation: [[[TBD]]].
"
  {:arglists '([if-pred then-part if-pred then-part  ...])}
  [& args]

  (-> (fn [type-map]
        (let [make-antecedent pred:exception->false
              make-consequent #(-> %
                                   force-all-of
                                   :condensed-type-descriptions
                                   (lifting/lift-type type-map))
              adjusted-pairs (->> args
                                  (alternately make-antecedent make-consequent)
                            (partition 2))]
          (implies:mkfn:from-adjusted adjusted-pairs)))
      includes/as-type-expander))
