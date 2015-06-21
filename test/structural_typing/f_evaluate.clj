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

  (let [f ( ( (fn [a] (fn [b] (fn [c] (+ a b c)))) 1) 2)]
    (subject/friendly-name f) => "your custom predicate")

  (let [f ( ( (fn [a] (fn [b] (fn my:tweedle-dum [c] (+ a b c)))) 1) 2)]
    (subject/friendly-name f) => "my:tweedle-dum"))

(fact "evaluating a predicate"
  (facts "pure functions"
    (let [lifted (subject/lift even?)]
      (lifted {:leaf-value 3}) => e/left?
      (lifted {:leaf-value 4}) => e/right?

      (let [result (e/run-left (lifted {:leaf-value 3 :whole-value {:x 3} :path [:x]}))]
        result => {:predicate even?
                   :predicate-string "core/even?"
                   :path [:x]
                   :leaf-value 3
                   :whole-value {:x 3}
                   :error-explainer subject/default-error-explainer}
        ( (:error-explainer result) result) => ":x should be `core/even?`; it is `3`")

      (fact "what about a function with a given name?"
        (let [lifted (subject/lift (fn greater-than-3 [n] (> n 3)))
              result (e/run-left (lifted {:leaf-value 3 :path [:x]}))]
          ( (:error-explainer result) result) => ":x should be `greater-than-3`; it is `3`"))

      (fact "functions tagged with names"
        (let [lifted (subject/lift (->> (fn [n] (> n 3)) (subject/show-as "three")))
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
    (let [lifted (subject/lift #'even?)
          result (e/run-left (lifted {:leaf-value "string"}))]
      result => {:predicate #'even?
                 :predicate-string "even?"
                 :leaf-value "string"
                 :error-explainer subject/default-error-explainer}))
    
    
  (fact "constructing a custom predicate"
    (fact "you can override the predicate-string argument"
      
      (let [my-variant-predicate (->> even? (subject/show-as "evenish"))
            lifted (subject/lift my-variant-predicate)
            result (e/run-left (lifted {:leaf-value "string" :path [:x]}))]
        result => (contains {:predicate (exactly even?)
                             :predicate-string "evenish"
                             :path [:x]
                             :leaf-value "string"
                             :error-explainer subject/default-error-explainer})
        ( (:error-explainer result) result) => ":x should be `evenish`; it is `\"string\"`"))

    (fact "you can override the error-explainer argument"
      (let [explainer (fn [{:keys [predicate-string
                                   path
                                   leaf-value]}]
                        (format "%s - %s - %s" path predicate-string leaf-value))
            lifted (subject/lift (subject/explain-with explainer even?))
            result (e/run-left (lifted {:leaf-value 3 :path [:x]}))]

        result => (contains {:predicate (exactly even?)
                             :predicate-string "core/even?"
                             :path [:x]
                             :leaf-value 3})
        ( (:error-explainer result) result) => "[:x] - core/even? - 3"))

    (fact "you can add arbitrary arguments" 
      (let [member (fn [& args]
                     (->> #(some (set args) %)
                          (subject/explain-with
                           (fn [{:keys [path leaf-value]}]
                             (format "%s (`%s`) should be a member of %s",
                                     (subject/friendly-path path)
                                     (pr-str leaf-value)
                                     (pr-str args))))))
            lifted (subject/lift (member 1 2 3))
            result (e/run-left (lifted {:leaf-value 8 :path [:x]}))]
        result => (contains {:predicate fn?
                             :predicate-string "your custom predicate"
                             :path [:x]
                             :leaf-value 8
                             :error-explainer fn?})

        ((:error-explainer result) result) => ":x (`8`) should be a member of (1 2 3)"))))

;; (future-fact "evaluating multiple predicates short circuits"
;;   (let [lifted (subject/life-predicates [pos? even?] [:x])
;;         result (e/run-left (lifted -1 {:x 1}))]
;;     result => {:predicate pos?
;;                :predicate-string "core/pos?"
;;                :path [:x]
;;                :leaf-value 3
;;                :whole-value {:x 3}
;;                :error-explainer subject/default-error-explainer}
    
