(ns structural-typing.evaluate
  (:use blancas.morph.core
        blancas.morph.monads)
  (:require [clojure.repl :as repl]))

(defn show-as [name f]
  (vary-meta f assoc ::predicate-name name))

(defn friendly-name [f]
  (let [base-name (->> (pr-str f)
                       repl/demunge
                       (re-matches #"^..([^ ]+).*")
                       second)
        meta-name (::predicate-name (meta f))
        generated? (second (re-find #".*/(.*)--[0-9]+" base-name))]
    (cond meta-name           meta-name
          (keyword? f)        f
          (= generated? "fn") "your custom predicate"
          generated?          generated?
          (var? f)            (str (:name (meta f)))
          base-name           base-name
          :else               "<could not find predicate name>")))

(defn friendly-path [path]
  (if (= 1 (count path)) (first path) path))

(defn default-error-explainer [{:keys [path predicate-string leaf-value]}]
  (format "%s should be `%s`; it is `%s`"
          (friendly-path path)
          predicate-string
          (pr-str leaf-value)))

(defn lift
  ([pred base-diagnostics]
     (let [diagnostics (assoc (merge {:error-explainer default-error-explainer
                                      :predicate-string (friendly-name pred)}
                                     base-diagnostics)
                              :predicate pred)]
       (fn [leaf-value-context]
         (make-either (merge diagnostics leaf-value-context)
                      (fn [x] (try (pred x) (catch Exception ex false)))
                      (:leaf-value leaf-value-context)))))
  ([pred]
     (lift pred {})))


(defn one-path [structure [path predicates]]
  
  )
   


(defn one-type [structure type-map]
  (map #(one-path structure %) type-map))
