(ns structural-typing.mechanics.f-lifting-predicates
  (:require [structural-typing.mechanics.lifting-predicates :as subject]
            [structural-typing.mechanics.m-preds :as pred]
            [structural-typing.api.path :as path]
            [structural-typing.api.defaults :as default])
  (:require [com.rpl.specter :refer [ALL]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))



(fact "lifted predicates are given the value to test in a map and return an Either"
  (let [lifted (subject/lift even?)]
    (lifted {:leaf-value 3}) => e/left?
    (lifted {:leaf-value 4}) => e/right?
    
    (let [oopsie (e/run-left (lifted {:leaf-value 3}))]
      oopsie => (contains {:predicate (exactly even?)
                           :leaf-value 3}))))
    
;; Some shorthand for remaining tests
(defn lift-and-run [original-pred value]
  (let [lifted (subject/lift original-pred)]
    (e/run-left (lifted {:leaf-value value}))))

(facts "The oopsie gives the information needed to produce an error string"
  (fact "a named function is shown in a friendly way"
    (lift-and-run even? 3) => (contains {:predicate-string "even?"
                                         :leaf-value 3
                                         :predicate-explainer default/default-predicate-explainer}))
    
  (fact "an anonymous lambda prints as something innocuous"
    (lift-and-run #(> 1 %) 3)
    => (contains {:predicate-string "<custom-predicate>"
                  :leaf-value 3
                  :predicate-explainer default/default-predicate-explainer}))

  (fact "a named lambda has its name used as the predicate-string"
    (lift-and-run (fn greater-than-3 [n] (> n 3)) 3)
    => (contains {:predicate-string "greater-than-3"
                  :predicate-explainer default/default-predicate-explainer}))
    
  (fact "functions can be tagged with names"
    (lift-and-run (->> (fn [n] (> n 3))
                       (pred/show-as "three"))
                  3)
    => (contains {:predicate-string "three"
                  :predicate-explainer default/default-predicate-explainer}))

  (fact "you can override the predicate-explainer"
    (let [explainer (fn [{:keys [predicate-string
                                 path
                                 leaf-value]}]
                      (format "%s - %s - %s" path predicate-string leaf-value))]
      (lift-and-run (pred/explain-with explainer even?) 3)
      => (contains {:predicate (exactly even?) ; original predicate
                    :predicate-string "even?"  
                    :leaf-value 3
                    :predicate-explainer (exactly explainer)})))
  
  (fact "lifting a var is much like lifting a function"
    (lift-and-run #'even? 3)
    => (contains {:predicate #'even?
                  :predicate-string "even?"
                  :predicate-explainer default/default-predicate-explainer})))


(fact "there are cases where predicate evaluation is overridden"
  (fact "an exception produces an oopsie"
    ( (subject/lift #(> 1 %)) {:leaf-value "string"}) => e/left?)

  (fact "a nil value NEVER produces an oopsie"
    ( (subject/lift even?) {:leaf-value nil}) => e/right?))

(fact "lifting does nothing to an already-lifted predicate"
  (let [lifted (subject/lift #'even?)
        lifted-again (subject/lift lifted)]
    (identical? lifted lifted-again) => true))
    

