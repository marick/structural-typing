(ns ^:no-doc structural-typing.guts.preds.lifted
  "A `lifted` predicate is one that takes an oopsie containing a value, 
   rather than the value directly. Failure is a Left containing the oopsie.
   Success is a Right containing the value."
  (:require [such.function-makers :as mkfn]
            [structural-typing.guts.preds.annotated :as annotated]
            ))


(def lifted-mark ::lifted)
(defn mark-as-lifted [pred]
  (vary-meta pred assoc lifted-mark true))
(defn already-lifted? [pred]
  (lifted-mark (meta pred)))

(prn "Todo: move some functions in lifted.")
(defn pred->about-pred [pred]
  (hash-map :explainer (annotated/get-explainer pred)
            :predicate-string (annotated/get-predicate-string pred)
            :predicate (annotated/get-predicate pred)))

(defn ->oopsie [& abouts]
  (apply merge abouts))

;; even the lifted function should print nicely
(def name-lifted-predicate annotated/replace-predicate-string)

