(ns ^:no-doc structural-typing.guts.preds.lifted
  "A `lifted` predicate is one that takes an oopsie containing a value, 
   rather than the value directly. Failure is a Left containing the oopsie.
   Success is a Right containing the value."
  (:require [blancas.morph.monads :as e]
            [such.function-makers :as mkfn]
            [structural-typing.guts.preds.annotated :as annotated]
            ))


(def lifted-mark ::lifted)
(defn mark-as-lifted [pred]
  (vary-meta pred assoc lifted-mark true))
(defn- already-lifted? [pred]
  (lifted-mark (meta pred)))

(defn- pred->about-pred [pred]
  (hash-map :predicate-explainer (annotated/get-explainer pred)
            :predicate-string (annotated/get-predicate-string pred)
            :predicate (annotated/get-predicate pred)))

(defn ->oopsie [& abouts]
  (apply merge abouts))

(defn- oopsie->either [{:keys [predicate leaf-value] :as oopsie}]
  (e/make-either oopsie
                 (mkfn/pred:exception->false predicate)
                 leaf-value))

;; even the lifted function should print nicely
(def name-lifted-predicate annotated/replace-predicate-string)

(defn- optional-evaluation [{:keys [predicate leaf-value] :as oopsie}]
  (cond (nil? leaf-value)
        (e/right :missing-optional-leaf-ignored) ; Note: cannot *be* nil - that will turn into a Left

        ((mkfn/pred:exception->false predicate) leaf-value)
        (e/right leaf-value)

        :else
        (e/left oopsie)))

(defn lift* [pred]
  (-> (fn [about-call]
        (optional-evaluation (->oopsie (pred->about-pred pred) about-call)))
      mark-as-lifted
      (name-lifted-predicate (annotated/get-predicate-string pred))))

(defn lift [pred]
  (if (already-lifted? pred)
    pred
    (lift* pred)))

