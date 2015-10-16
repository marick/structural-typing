(ns structural-typing.preds
  "Predefined predicates that are not imported into `structural-typing.type`."
  (:use structural-typing.clojure.core)
  (:require [structural-typing.assist.lifting :as lifting]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.assist.predicate-defining :as pdef]
            [structural-typing.guts.type-descriptions.includes :as includes]
            [such.readable :as readable])
  (:use structural-typing.assist.special-words))

(defn member
  "Produce a predicate that's false when applied to a value not a
   member of `coll`. The explainer associated with `member` prints
   `coll`.
     
         (type! :small-primes {:n (member [2 3 5 7])})
"
  [coll]
  (pdef/compose-predicate
   (format "(member %s)" (readable/value-string coll))
   #(boolean ((set coll) %))
   (pdef/should-be "%s should be a member of `%s`; it is `%s`" coll)))

(defn exactly
  "Produce a predicate that's true iff the value it's applied to
   is `expected`.
     
         (type! :V5 {:version (exactly 5)})
"
  [expected]
  (pdef/compose-predicate
   (format "(exactly %s)" (readable/value-string expected))
   (partial = expected)
   (pdef/should-be "%s should be exactly `%s`; it is `%s`" expected)))


(defn- key-differences [expected-keycoll actual-value]
  (let [actual-set (set (keys actual-value))
        expected-keyset (set expected-keycoll)
        extras (set-difference actual-set expected-keyset)
        missing (set-difference expected-keyset actual-set)]
    (cond (not-empty? extras) [:actual-has-extras extras]
          (not-empty? missing) [:actual-missing-required missing]
          :else [:ok])))

(defn at-most-keys
  "Produce a predicate that's false when applied to a value with keys other than
   those given in `coll`. Note: the value may be *missing* keys in `coll`. See [[exactly-keys]].
   
         user=> (type! :X {:v1 string?
                           :v2 integer?}
                          (at-most-keys :v1 :v2))
         user=> (built-like :X {:v1 \"apple\"})
         => {:v1 \"apple\"}
         user=> (built-like :X {:v1 \"apple\" :v2 3})
         => {:v1 \"apple\", :v2 3}
         user=> (built-like :X {:v1 \"apple\" :v2 3, :actual-is-too-big true})
         Value has extra keys: #{:actual-is-too-big}; it is {:v1 \"apple\", ...
         => nil
   
   Note: this predicate works only with keys, not paths."
  [& coll]
  (letfn []
    (->> (fn [actual]
           (let [[status _] (key-differences coll actual)]
             (not= status :actual-has-extras)))
         (show-as (cl-format nil "(at-most-keys ~{~A~^ ~})" coll))
         (explain-with (fn [oopsie]
                         (format "%s has extra keys: %s; it is %s"
                                 (oopsie/friendly-path oopsie)
                                 (second (key-differences coll (:leaf-value oopsie)))
                                 (:leaf-value oopsie)))))))

(defn exactly-keys
  "Produce a predicate that's false when applied to a value with keys at all different than
   those given in `coll`. See [[at-most-keys]] for a variant that allows the value to be
   missing some of the `coll` keys.
   
        user=> (type! :X {:v1 string?
                          :v2 integer?}
                         (exactly-keys :v1 :v2))
        user=> (built-like :X {:v1 \"apple\"})
        => Value has missing keys: #{:v2}; it is {:v1 \"apple\"}
        nil
        user=> (built-like :X {:v1 \"apple\" :v2 3})
        {:v1 \"apple\", :v2 3}
        user=> (built-like :X {:v1 \"apple\" :v2 3, :actual-is-too-big true})
        Value has extra keys: #{:actual-is-too-big}; it is {:v1 \"apple\", ...
        => nil

   
   Note: this predicate works only with keys, not paths."
  [& coll]
  (letfn []
    (->> (fn [actual]
           (let [[status _] (key-differences coll actual)]
             (= status :ok)))
         (show-as (cl-format nil "(exactly-keys ~{~A~^ ~})" coll))
         (explain-with (fn [oopsie]
                         (let [[status keys] (key-differences coll (:leaf-value oopsie))
                               fmt (if (= status :actual-has-extras)
                                     "%s has extra keys: %s; it is %s"
                                     "%s has missing keys: %s; it is %s")]
                         (format fmt
                                 (oopsie/friendly-path oopsie)
                                 keys
                                 (:leaf-value oopsie))))))))

(defn ^:no-doc matches
  "Doc"
  [expected]
  (pdef/compose-predicate
   (format "(matches %s)" (readable/value-string expected))
   (fn [actual]
     (cond (every? regex? [actual expected])
           (= (str actual) (str expected))
           
           (regex? expected)
           (boolean (re-find expected actual))

           :else
           (= actual expected)))
   (pdef/should-be "%s should match `%s`; it is `%s`" expected)))


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
   page in the user documentation: http://bit.ly/1LeLTy9.

   Warning: I am considering making this function more powerful. I
   think the change won't be breaking, but it might be.
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
