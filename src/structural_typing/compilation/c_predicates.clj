(ns structural-typing.compilation.c-predicates
  (:require [clojure.repl :as repl]
            [blancas.morph.monads :as e]
            [structural-typing.api.path :as path]
            [structural-typing.api.defaults :as defaults]))

(defn friendly-name [f]
  (let [base-name (->> (pr-str f)
                       repl/demunge
                       (re-matches #"^..([^ ]+).*")
                       second)
        meta-name (::predicate-string (meta f))
        generated? (second (re-find #".*/(.*)--[0-9]+" base-name))]
    (cond (keyword? f)        f
          (= generated? "fn") "your custom predicate"
          generated?          generated?
          (var? f)            (str (:name (meta f)))
          base-name           base-name
          :else               "<could not find predicate name>")))



(defn precache [f k v]
  (if (contains? (meta f) k)
    f
    (vary-meta f assoc k v)))

(defn best-predicate-choice [f]
  (get (meta f) ::original-predicate f))

(defn show-as [name f]
  (-> f
      (precache ::original-predicate f)
      (vary-meta assoc ::predicate-string name)))

(defn best-predicate-string-choice [f]
  (get (meta f) ::predicate-string (friendly-name f)))

(defn explain-with [explainer f]
  (-> f
      (precache ::original-predicate f)
      (precache ::predicate-string (friendly-name f))
      (vary-meta assoc ::error-explainer explainer)))

(defn best-explainer-choice [f]
  (get (meta f) ::error-explainer defaults/default-error-explainer))

(defn lift [pred]
  (let [diagnostics {:error-explainer (best-explainer-choice pred)
                     :predicate-string (best-predicate-string-choice pred)
                     :predicate (best-predicate-choice pred)}]
    (fn [leaf-value-context]
      (e/make-either (merge diagnostics leaf-value-context)
                     (fn [x] (try (pred x) (catch Exception ex false)))
                     (:leaf-value leaf-value-context)))))



