(ns structural-typing.f-evaluate
  (:require [structural-typing.evaluate :as subject]
            [structural-typing.predicates :as p])
  (:require [com.rpl.specter :refer [ALL]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))


(fact "friendly-name"
  (subject/friendly-name even?) => "core/even?"
  (subject/friendly-name (fn [])) => "your custom predicate"
  (subject/friendly-name :key) => :key
  (subject/friendly-name #'even?) => "even?"

  (subject/friendly-name (subject/show-as "odd!!!!!" even?)) => "odd!!!!!"

  (let [f ( ( (fn [a] (fn [b] (fn [c] (+ a b c)))) 1) 2)]
    (subject/friendly-name f) => "your custom predicate")

  (let [f ( ( (fn [a] (fn [b] (fn my:tweedle-dum [c] (+ a b c)))) 1) 2)]
    (subject/friendly-name f) => "my:tweedle-dum"))

(fact "evaluating a predicate"
  (facts "pure functions"
    (let [lifted (subject/lift even? [:x])]
      (lifted 3 {:x 3}) => e/left?
      (lifted 4 {:x 4}) => e/right?

      (let [result (e/run-left (lifted 3 {:x 3}))]
        result => {:predicate even?
                   :predicate-string "core/even?"
                   :path [:x]
                   :leaf-value 3
                   :whole-value {:x 3}
                   :error-explainer subject/default-error-explainer}
        ( (:error-explainer result) result) => ":x should be `core/even?`; it is `3`")

      (fact "what about a function with a given name?"
        (let [lifted (subject/lift (fn greater-than-3 [n] (> n 3)) [:x])
              result (e/run-left (lifted 3 ..irrelevant..))]
          ( (:error-explainer result) result) => ":x should be `greater-than-3`; it is `3`"))

      (fact "functions tagged with names"
        (let [lifted (subject/lift (subject/show-as "three" (fn [n] (> n 3))) [:x])
              result (e/run-left (lifted 3 ..irrelevant..))]
          ( (:error-explainer result) result) => ":x should be `three`; it is `3`"))))

  (fact "lifting a var - same except for better names"
    (let [lifted (subject/lift #'even? [:x])
          result (e/run-left (lifted 3 {:x 3}))]
      result => {:predicate #'even?
                 :predicate-string "even?"
                 :path [:x]
                 :leaf-value 3
                 :whole-value {:x 3}
                 :error-explainer subject/default-error-explainer}
        ( (:error-explainer result) result) => ":x should be `even?`; it is `3`"))

  (fact "a predicate that throws"
    (let [lifted (subject/lift #'even? [:x])
          result (e/run-left (lifted "string" {:x "string"}))]
      result => {:predicate #'even?
                 :predicate-string "even?"
                 :path [:x]
                 :leaf-value "string"
                 :whole-value {:x "string"}
                 :error-explainer subject/default-error-explainer}))
    
    
  (future-fact "constructing a custom predicate - easy case")
)


;; (fact "evaluating multiple predicates short circuits"
;;   (let [lifted (subject/life-predicates [pos? even?] [:x])
;;         result (e/run-left (lifted -1 {:x 1}))]
;;     result => {:predicate pos?
;;                :predicate-string "core/pos?"
;;                :path [:x]
;;                :leaf-value 3
;;                :whole-value {:x 3}
;;                :error-explainer subject/default-error-explainer}
    
