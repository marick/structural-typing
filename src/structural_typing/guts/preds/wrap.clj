(ns ^:no-doc structural-typing.guts.preds.wrap
  (:use structural-typing.clojure.core)
  (:require [such.readable :as readable]
            [structural-typing.guts.expred :as expred]
            [structural-typing.guts.oopsie :as oopsie]))

;; TODO: make readable have the "ensure-meta" behavior)


(defn gm [f k default] (get (meta f) k default))
(defn vm [f k v] (vary-meta f assoc k v))
(defn ensure-meta [f k v] (if (contains? (meta f) k) f (vm f k v)))


(defn get-predicate-string [f] (readable/fn-string f))
(defn get-predicate [f]        (gm f ::original-predicate f))
(defn get-explainer [f]        (gm f ::predicate-explainer expred/default-predicate-explainer))


(defn stash-defaults [f]
  (-> f
      (ensure-meta ::original-predicate f)
      (readable/rename (readable/fn-string f))))

(defn replace-predicate-string [f name] (readable/rename f name))
(defn replace-explainer [f explainer] (vm f ::predicate-explainer explainer))
  
(defn show-as 
  "Associate the given `name` string with the predicate for use when predicate failures
   are explained.
     
         (->> (partial >= 3) (show-as \"less than 3\"))
"
  [name predicate]
  (when (fn? name) (boom! "First arg is a function. You probably got your args reversed."))
  (when-not (string? name) (boom! "First arg must be a string: %s %s" name predicate))
  (-> predicate
      stash-defaults
      (replace-predicate-string name)))

(defn explain-with
  "After the `predicate` fails, the failure will need to be explained. Arrange for
   the `explainer` function to be called with the [[oopsie]] that results from the
   failure.
   
        (explain-with \"too small\" #(< (count %) 54))
"
  [explainer predicate]
  (-> predicate
      stash-defaults
      (replace-explainer explainer)))




(def ^:private lifted-mark ::lifted)
(defn mark-as-lifted [pred]
  (vary-meta pred assoc lifted-mark true))
(defn already-lifted? [pred]
  (lifted-mark (meta pred)))


(defn ->expred [pred]
  (expred/boa (get-predicate pred)
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
            (vector (oopsie/parts->oopsie expred exval))))
        mark-as-lifted
        (give-lifted-predicate-a-nice-string expred))))

(defn lift
  ([pred protection-subtractions]
     (if (already-lifted? pred)
       pred
       (lift-expred (->expred pred) protection-subtractions)))
  ([pred]
     (lift pred [])))
  
