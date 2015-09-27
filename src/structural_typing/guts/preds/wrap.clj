(ns ^:no-doc structural-typing.guts.preds.wrap
  (:use structural-typing.clojure.core)
  (:require [such.readable :as readable]
            [such.metadata :as meta]
            [structural-typing.assist.defaults :as defaults]
            [structural-typing.guts.expred :as expred]))

;; TODO: make readable have the "ensure-meta" behavior

;; TODO: This should really be two files: one for lifting behavior and one for annotating-via-metadata


(defn ensure-meta [f k v] (if (contains? (meta f) k) f (meta/assoc f k v)))


(defn get-predicate-string [f] (readable/fn-string f))
(defn get-predicate [f]        (meta/get f ::original-predicate f))
(defn get-explainer [f]        (meta/get f ::predicate-explainer defaults/default-predicate-explainer))


(defn stash-defaults [f]
  (-> f
      (ensure-meta ::original-predicate f)
      (readable/rename (readable/fn-string f))))

(defn replace-predicate-string [f name] (readable/rename f name))
(defn replace-explainer [f explainer] (meta/assoc f ::predicate-explainer explainer))
  


(def ^:private lifted-mark ::lifted)
(defn mark-as-lifted
  "A pred so marked is not lifted again. You can call [[lift-pred]] safely many times."
  [pred]
  (vary-meta pred assoc lifted-mark true))

(defn already-lifted? [pred]
  (lifted-mark (meta pred)))


(defn ->expred [pred]
  (expred/->ExPred (get-predicate pred)
                   (get-predicate-string pred)
                   (get-explainer pred)))


(defn- mkfn:optional [pred]
  (fn [value]
    (if (nil? value)
      true
      (pred value))))

(defn give-lifted-predicate-a-nice-string [pred expred]
  (replace-predicate-string pred (:predicate-string expred)))

(defn protect-pred [pred protection-subtractions]
  (when-not (empty? (remove #{:allow-exceptions :check-nil} protection-subtractions))
    (throw (new Exception (str protection-subtractions))))
  (-> pred
      (cond-> (not (any? #{:allow-exceptions} protection-subtractions)) pred:exception->false
              (not (any? #{:check-nil} protection-subtractions)) mkfn:optional)))

(defn lift-expred [expred protection-subtractions]
  (let [protected (protect-pred (:predicate expred) protection-subtractions)]
    (-> (fn [exval]
          (if (protected (:leaf-value exval))
            []
            (vector (merge expred exval))))
        mark-as-lifted
        (give-lifted-predicate-a-nice-string expred))))

(defn lift
  ([pred protection-subtractions]
     (if (already-lifted? pred)
       pred
       (lift-expred (->expred pred) protection-subtractions)))
  ([pred]
     (lift pred [])))
  
