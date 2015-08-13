(ns ^:no-doc structural-typing.guts.shapes.pred
  (:require [such.readable :as readable]
            [structural-typing.guts.frob :as frob]
            [structural-typing.pred-writing.shapes.expred :as expred]
            [structural-typing.pred-writing.defaults :as defaults]
))

;; TODO: make readable have the "ensure-meta" behavior)


(defn gm [f k default] (get (meta f) k default))
(defn vm [f k v] (vary-meta f assoc k v))
(defn ensure-meta [f k v] (if (contains? (meta f) k) f (vm f k v)))


(defn get-predicate-string [f] (readable/fn-string f))
(defn get-predicate [f]        (gm f ::original-predicate f))
(defn get-explainer [f]        (gm f ::predicate-explainer defaults/default-predicate-explainer))


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
  (when (fn? name) (frob/boom! "First arg is a function. You probably got your args reversed."))
  (when-not (string? name) (frob/boom! "First arg must be a string: %s %s" name predicate))
  (-> predicate
      stash-defaults
      (replace-predicate-string name)))

(defn explain-with
  "After the `predicate` fails, the failure will need to be explained. Arrange for
   the `explainer` function to be called with the [[oopsie]] that results from the
   failure."
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

