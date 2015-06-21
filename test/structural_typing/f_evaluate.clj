(ns structural-typing.f-evaluate
  (:require [structural-typing.evaluate :as subject]
            [structural-typing.api.predicates :as p])
  (:require [com.rpl.specter :refer [ALL]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))



;; (fact "evaluating multiple predicates checks each of them"
;;   (let [lifted (subject/lift-predicates [pos? even?])
;;         run (fn [x] (e/lefts (lifted {:leaf-value x})))]
;;     (run 8) => empty?

;;     (run -2) => (just (contains {:predicate-string "core/pos?"
;;                                  :predicate pos?
;;                                  :leaf-value -2}))
;; ))
    
