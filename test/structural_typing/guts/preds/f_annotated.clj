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
