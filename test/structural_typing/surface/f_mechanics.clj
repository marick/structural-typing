(ns structural-typing.surface.f-mechanics
  (:require [structural-typing.surface.mechanics :as subject]
            [such.readable :as readable]
            [structural-typing.surface.defaults :as default]
            [structural-typing.guts.paths.elements :refer [ALL]]
            [structural-typing.guts.preds.annotated :as annotated])
  (:use midje.sweet))

(fact "predicates are typically wrapped with handlers for nils and exceptions"
  (fact "exceptions"
    (even? "string") => (throws)
    ( (subject/lift-pred-map {:predicate even?}          ) {:leaf-value "string"}) => (throws)
    ( (subject/lift-pred-map {:predicate even?} :catching) {:leaf-value "string"})
    => (just (contains {:leaf-value "string"})))
    ( (subject/lift-pred-map {:predicate even?} :catching :optional) {:leaf-value "string"})
    => (just (contains {:leaf-value "string"}))

  (fact "exceptions"
    (even? "string") => (throws)
    ( (subject/lift-pred-map {:predicate even?}          ) {:leaf-value nil}) => (throws)
    ( (subject/lift-pred-map {:predicate even?} :optional) {:leaf-value nil}) => []
    ( (subject/lift-pred-map {:predicate even?} :catching :optional) {:leaf-value nil}) => []))


(defn lift-and-run [pred value]
  ( (subject/lift pred) {:leaf-value 3}))


(fact "lifted predicates are given the value to test in a map and return oopsies"
  (let [lifted (subject/lift even?)]
    (lifted {:leaf-value 3}) => (just (contains {:leaf-value 3}))
    (lifted {:leaf-value 4}) => empty?

    (fact "information about the predicate is contained in the failed oopsie"
      (lifted {:leaf-value 3}) => (just (contains {:predicate (exactly even?)})))

    (fact "any information passed in is retained in the output"
      (lifted {:leaf-value 3 :anything "here"}) => (just (contains {:anything "here"})))))

(facts "The oopsie contains gives the information needed to produce an error string"
  (fact "a named function is shown in a friendly way"
    (lift-and-run even? 3) => (just (contains {:predicate-string "even?"
                                               :leaf-value 3
                                               :explainer default/default-predicate-explainer})))

  (fact "Lifting depends on clever predicate name extraction"
    (let [any-old-predicate (constantly false)]
      (lift-and-run any-old-predicate 3) => (just (contains {:predicate-string ..name..}))
      (provided
        (annotated/get-predicate-string any-old-predicate) => ..name..))))
  
(fact "lifting a var is like lifting a function"
  (lift-and-run #'even? 3)
  => (just (contains {:predicate #'even?
                      :predicate-string "even?"
                      :explainer default/default-predicate-explainer})))

(fact "lifting can be told convert nil values to success"
  ( (subject/lift (complement nil?)) {:leaf-value nil}) =not=> empty?
  ( (subject/lift (complement nil?) :optional) {:leaf-value nil}) => empty?)

(fact "lifting can be told to convert exceptions into an oopsie"
  (even? nil?) => (throws Exception)
  ( (subject/lift even?) {:leaf-value nil}) => (throws Exception)
  ( (subject/lift even? :catches) {:leaf-value nil}) =not=> empty?)

    

(fact "an already-lifted predicate is left unchanged"
  (let [once (subject/lift even?)
        twice (subject/lift once)]
    (identical? once twice) => true)

  (fact "note that means that additions cannot be changed"
    (let [once (subject/lift even?)
          twice (subject/lift once :catching)]
      (identical? once twice) => true)))
