(ns structural-typing.mechanics.fm-lifting-predicates
  (:require [structural-typing.mechanics.m-lifting-predicates :as subject]
            [structural-typing.api.predicates :as pred]
            [structural-typing.api.path :as path]
            [structural-typing.api.defaults :as defaults])
  (:require [com.rpl.specter :refer [ALL]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))


(fact "friendly-name"
  (subject/friendly-name even?) => "even?"
  (subject/friendly-name (fn [])) => "your custom predicate"
  (subject/friendly-name :key) => ":key"
  (subject/friendly-name #'even?) => "even?"

  (let [f ( ( (fn [a] (fn [b] (fn [c] (+ a b c)))) 1) 2)]
    (subject/friendly-name f) => "your custom predicate")

  (let [f ( ( (fn [a] (fn [b] (fn my:tweedle-dum [c] (+ a b c)))) 1) 2)]
    (subject/friendly-name f) => "my:tweedle-dum"))


(fact "lifting a predicate"
  (facts "pure functions"
    (let [lifted (subject/lift even?)]
      (lifted {:leaf-value 3}) => e/left?
      (lifted {:leaf-value 4}) => e/right?

      (let [result (e/run-left (lifted {:leaf-value 3 :whole-value {:x 3} :path [:x]}))]
        result => {:predicate even?
                   :predicate-string "even?"
                   :path [:x]
                   :leaf-value 3
                   :whole-value {:x 3}
                   :error-explainer defaults/default-error-explainer}
        ( (:error-explainer result) result) => ":x should be `even?`; it is `3`")

      (fact "what about a function with a given name?"
        (let [lifted (subject/lift (fn greater-than-3 [n] (> n 3)))
              result (e/run-left (lifted {:leaf-value 3 :path [:x]}))]
          ( (:error-explainer result) result) => ":x should be `greater-than-3`; it is `3`"))

      (fact "functions tagged with names"
        (let [lifted (subject/lift (->> (fn [n] (> n 3)) (pred/show-as "three")))
              result (e/run-left (lifted {:leaf-value 3 :path [:x :y]}))]
          ( (:error-explainer result) result) => "[:x :y] should be `three`; it is `3`"))))

  (fact "lifting a var - same except for better names"
    (let [lifted (subject/lift #'even?)
          result (e/run-left (lifted {:leaf-value 3 :path [:x]}))]
      result => (contains {:predicate #'even?
                           :predicate-string "even?"
                           :path [:x]
                           :leaf-value 3})
      ( (:error-explainer result) result) => ":x should be `even?`; it is `3`"))

  (fact "a predicate that throws"
    (let [lifted (subject/lift #(> 1 %))
          result (e/run-left (lifted {:leaf-value "string"}))]
      result => (contains {:predicate-string "your custom predicate"
                           :leaf-value "string"
                           :error-explainer defaults/default-error-explainer})))

  (fact "predicates are true of `nil` values"
    (let [lifted (subject/lift #'even?)
          result (e/run-left (lifted {:leaf-value nil}))]
      result => empty?))

  (fact "lifting does nothing to an already-lifted predicate"
    (let [lifted (subject/lift #'even?)
          lifted-again (subject/lift lifted)]
      (prn lifted)
      (prn lifted-again)
      (identical? lifted lifted-again) => true))
    
  (fact "constructing a custom predicate"
    (fact "you can override the predicate-string argument"
      
      (let [my-variant-predicate (->> even? (pred/show-as "evenish"))
            lifted (subject/lift my-variant-predicate)
            result (e/run-left (lifted {:leaf-value "string" :path [:x]}))]
        result => (contains {:predicate (exactly even?)
                             :predicate-string "evenish"
                             :path [:x]
                             :leaf-value "string"
                             :error-explainer defaults/default-error-explainer})
        ( (:error-explainer result) result) => ":x should be `evenish`; it is `\"string\"`"))

    (fact "you can override the error-explainer argument"
      (let [explainer (fn [{:keys [predicate-string
                                   path
                                   leaf-value]}]
                        (format "%s - %s - %s" path predicate-string leaf-value))
            lifted (subject/lift (pred/explain-with explainer even?))
            result (e/run-left (lifted {:leaf-value 3 :path [:x]}))]

        result => (contains {:predicate (exactly even?)
                             :predicate-string "even?"
                             :path [:x]
                             :leaf-value 3})
        ( (:error-explainer result) result) => "[:x] - even? - 3"))))

