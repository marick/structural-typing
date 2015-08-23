(ns structural-typing.preds
  "All of the predefined predicates."
  (:require [structural-typing.pred-writing.lifting :as lifting]
            [structural-typing.pred-writing.shapes.oopsie :as oopsie]
            [structural-typing.guts.shapes.pred :as pred]
            [structural-typing.pred-writing.shapes.expred :as expred]
            [structural-typing.guts.frob :as frob]
            [such.readable :as readable]
            [such.function-makers :as mkfn]
            [such.types :as types]))

(defn- should-be [format-string expected]
  #(format format-string,
           (oopsie/friendly-path %)
           (readable/value-string expected)
           (readable/value-string (:leaf-value %))))

(defn- compose-predicate [name pred fmt-fn]
  (->> pred
       (pred/show-as name)
       (pred/explain-with fmt-fn)))

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
     (cond (every? types/regex? [actual expected])
           (= (str actual) (str expected))
           
           (types/regex? expected)
           (boolean (re-find expected actual))

           :else
           (= actual expected)))
   (should-be "%s should match `%s`; it is `%s`" expected)))


;;; More exotic predicate creation.


;; Too many places accept vectors or single arguments. This is an experiment
;; to disallow vectors in favor of a function (rather than having, as elsewhere,
;; a function that's just an alias for `vector`).

(defrecord AllOf [type-descriptions])
(defn all-of [& type-descriptions] (->AllOf type-descriptions))
(defn force-all-of [x]
  (if (instance? AllOf x)
    x
    (all-of x)))

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
  {:arglists '([if-pred type-description if-pred type-description...])}
  [& args]

  (let [make-antecedent mkfn/pred:exception->false
        make-consequent (comp lifting/nested-type->val-checker :type-descriptions force-all-of)
        adjusted-pairs (->> args
                            (frob/alternately make-antecedent make-consequent)
                            (partition 2))]
    (->> (fn [exval]
           (letfn [(adjust-path [oopsie]
                     (update oopsie :path #(into (:path exval) %)))]
             (reduce (fn [so-far [antecedent consequent]]
                       (if (antecedent (:leaf-value exval))
                         (into so-far (map adjust-path (consequent (:leaf-value exval))))
                         so-far))
                     []
                     adjusted-pairs)))
         pred/mark-as-lifted
         (pred/show-as "implies"))))
