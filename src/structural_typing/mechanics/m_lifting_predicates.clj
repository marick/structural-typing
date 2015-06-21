(ns structural-typing.mechanics.m-lifting-predicates
  (:require [clojure.repl :as repl]
            [clojure.string :as str]
            [blancas.morph.monads :as e]
            [structural-typing.api.path :as path]
            [structural-typing.api.defaults :as defaults]))

(defn friendly-name [f]
  (cond (var? f)
        (str (:name (meta f)))

        (fn? f)
        (let [basename (-> (str f)
                             repl/demunge
                             (str/split #"/")
                             last
                             (str/split #"@")
                             first
                             (str/split #"--[0-9]+$")
                             first)]
          (if (= basename "fn") "your custom predicate" basename))
        :else
        (str f)))




(defn ensure-meta [f k v]
  (if (contains? (meta f) k)
    f
    (vary-meta f assoc k v)))

(defn ensure-availability-of-predicate-string [f name]
  (ensure-meta f ::predicate-string name))
(defn- ensure-availability-of-original-predicate [f]
  (ensure-meta f ::original-predicate f))

(defn- choose-best [f k default] 
  (get (meta f) k default))
(defn choose-best-predicate [f]
  (choose-best f ::original-predicate f))
(defn choose-best-predicate-string [f]
  (choose-best f ::predicate-string (friendly-name f)))
(defn choose-best-explainer [f]
  (get (meta f) ::error-explainer defaults/default-error-explainer))


(defn replace-predicate-string [f name]
  (vary-meta f assoc ::predicate-string name))
  



(defn show-as [name f]
  (-> f
      ensure-availability-of-original-predicate
      (replace-predicate-string name)))

(defn explain-with [explainer f]
  (-> f
      ensure-availability-of-original-predicate
      (ensure-availability-of-predicate-string (friendly-name f))
      (vary-meta assoc ::error-explainer explainer)))

(defn lift [pred]
  (let [diagnostics {:error-explainer (choose-best-explainer pred)
                     :predicate-string (choose-best-predicate-string pred)
                     :predicate (choose-best-predicate pred)}]
    (fn [leaf-value-context]
      (e/make-either (merge diagnostics leaf-value-context)
                     (fn [x] (try (pred x) (catch Exception ex false)))
                     (:leaf-value leaf-value-context)))))



