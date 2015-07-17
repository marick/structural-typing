(ns ^:no-doc structural-typing.guts.mechanics.lifting-predicates
  (:require [blancas.morph.monads :as e]
            [such.readable :as readable]
            [such.function-makers :as mkfn]
            [structural-typing.guts.preds.annotated :as annotated]
            ))


(def lifted-mark ::lifted)
(defn mark-as-lifted [pred]
  (vary-meta pred assoc lifted-mark true))
(defn- already-lifted? [pred]
  (lifted-mark (meta pred)))

(defn lift* [pred count-nil-as-right]
  (let [pred-name (annotated/get-predicate-string pred)
        diagnostics {:predicate-explainer (annotated/get-explainer pred)
                     :predicate-string pred-name
                     :predicate (annotated/get-predicate pred)}]
    (-> (fn [{:keys [leaf-value] :as leaf-value-context}]
          (let [oopsie (merge diagnostics leaf-value-context)]
            ;; Blancas make-either objects to an INPUT of nil, not a predicate result of falsey.
            ;; This produces convolution.
            (if (and (nil? leaf-value) count-nil-as-right)
              (e/right "was nil") ; Note: cannot *be* nil - that will turn into a Left.
              (e/make-either oopsie
                             (mkfn/pred:exception->false pred)
                             leaf-value))))
        mark-as-lifted
        ;; even the lifted function prints nicely
        (annotated/replace-predicate-string pred-name))))

(defn lift [pred]
  (if (already-lifted? pred)
    pred
    (lift* pred :count-nil-as-right)))
