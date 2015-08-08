(ns structural-typing.guts.preds.f_annotated
  (:require [structural-typing.guts.preds.annotated :as subject]
            [structural-typing.guts.preds.lifted :refer [lift]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))

(fact "show-as and explain-with"
  (let [pred (->> even?
                  (subject/show-as "name")
                  (subject/explain-with :predicate-string))]
    (pred 0) => true
    (pred 1) => false

    (let [result (e/run-left ( (lift pred) {:leaf-value 1}))]
      ((:predicate-explainer result) result) => "name")

    ( (lift pred) {:leaf-value 2}) => e/right?))


(future-fact "Uncomment these tests")


;;   (fact "an anonymous lambda prints as something innocuous"
;;     (lift-and-run #(> 1 %) 3)
;;     => (contains {:predicate-string "<custom-predicate>"
;;                   :leaf-value 3
;;                   :predicate-explainer default/default-predicate-explainer}))

;;   (fact "a named lambda has its name used as the predicate-string"
;;     (lift-and-run (fn greater-than-3 [n] (> n 3)) 3)
;;     => (contains {:predicate-string "greater-than-3"
;;                   :predicate-explainer default/default-predicate-explainer}))
    
;;   (fact "functions can be tagged with names"
;;     (lift-and-run (->> (fn [n] (> n 3))
;;                        (pred/show-as "three"))
;;                   3)
;;     => (contains {:predicate-string "three"
;;                   :predicate-explainer default/default-predicate-explainer}))

;;   (fact "you can override the predicate-explainer"
;;     (let [explainer (fn [{:keys [predicate-string
;;                                  path
;;                                  leaf-value]}]
;;                       (format "%s - %s - %s" path predicate-string leaf-value))]
;;       (lift-and-run (explain-with explainer even?) 3)
;;       => (contains {:predicate (exactly even?) ; original predicate
;;                     :predicate-string "even?"  
;;                     :leaf-value 3
;;                     :predicate-explainer (exactly explainer)})))

