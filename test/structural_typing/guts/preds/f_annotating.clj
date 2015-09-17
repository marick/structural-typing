(ns structural-typing.guts.preds.f-annotating
  (:require [structural-typing.guts.preds.annotating :as subject]
            [structural-typing.assist.defaults :as default])
  (:use midje.sweet structural-typing.assist.testutil))

(fact "simple example"
  (let [pred (->> even?
                  (subject/show-as "name")
                  (subject/explain-with :predicate-string))]
    (pred 0) => true
    (pred 1) => false
    
    (explain-lifted pred (exval 1)) => ["name"]
    (lift-and-run pred (exval 2)) => empty?))


(fact "our two functions in context"
  (fact "an anonymous lambda prints as something innocuous"
    (lift-and-run #(> 1 %) (exval 3))
    => (just (contains {:predicate-string "<custom-predicate>"
                        :leaf-value 3
                        :explainer default/default-predicate-explainer})))

  (fact "a named lambda has its name used as the predicate-string"
    (lift-and-run (fn greater-than-3 [n] (> n 3)) (exval 3))
    => (just (contains {:predicate-string "greater-than-3"
                        :explainer default/default-predicate-explainer})))
  
  (fact "`show-as` lets you tag functions with names"
    (lift-and-run (->> (fn [n] (> n 3))
                       (subject/show-as "three"))
                  (exval 3))
    => (just (contains {:predicate-string "three"
                        :explainer default/default-predicate-explainer})))
  
  (fact "you can override the explainer"
    (let [explainer (fn [{:keys [predicate-string
                                 path
                                 leaf-value]}]
                      (format "%s - %s - %s" path predicate-string leaf-value))]
      (lift-and-run (subject/explain-with explainer even?) (exval 3))
      => (just (contains {:predicate (exactly even?) ; original predicate
                          :predicate-string "even?"
                          :leaf-value 3
                          :explainer (exactly explainer)})))))

