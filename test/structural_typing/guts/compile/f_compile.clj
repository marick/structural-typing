(ns structural-typing.guts.compile.f-compile
  (:require [com.rpl.specter :as specter])
  (:require [structural-typing.guts.compile.compile :as subject]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.guts.preds.wrap :as wrap])
  (:use midje.sweet
        structural-typing.assist.special-words))
    

(defn run-preds [preds input]
  ((subject/compile-predicates preds) input))

(fact "compile multiple predicates into a function that checks each of them"
  (let [input {:leaf-value 1 :whole-value {:x 1} :path [:x]}
        oopsies (run-preds [even? odd?] input)]
    oopsies => (just (contains (assoc input :predicate (exactly even?))))
    (oopsie/explanations oopsies) => [":x should be `even?`; it is `1`"])

  (let [input {:leaf-value -3 :whole-value {[:x :y] -3} :path [:x :y]}
        oopsies (run-preds [pos? #'even?] input)]
    oopsies => (just (contains (assoc input :predicate (exactly pos?)))
                    (contains (assoc input :predicate (exactly #'even?))))
    ;; Note they are sorted.
    (oopsie/explanations oopsies) => ["[:x :y] should be `even?`; it is `-3`"
                                      "[:x :y] should be `pos?`; it is `-3`"])

  (let [input {:leaf-value -3 :whole-value {[:x :y] -3} :path [:x :y]}
        oopsies (run-preds [(->> pos? (show-as "POS!"))] input)]
    (oopsie/explanations oopsies) => ["[:x :y] should be `POS!`; it is `-3`"])

  (let [input {:leaf-value "string" :whole-value {[:x] "string"} :path [:x]}
        oopsies (run-preds [pos?] input)]
    (oopsie/explanations oopsies) => [":x should be `pos?`; it is `\"string\"`"]))

    
    





