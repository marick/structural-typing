(ns ^:no-doc structural-typing.guts.preds.required-key
  "The sole predicate that is not optional has to be constructed differently."
  (:require [blancas.morph.monads :as e]
            [such.function-makers :as mkfn]
            [structural-typing.surface.oopsie :as oopsie]
            [structural-typing.guts.preds.lifted :as lifted]
            ))

(defn- explainer [oopsie]
  (format "%s must exist and be non-nil" (oopsie/friendly-path oopsie)))

(def ^:private pname "required-key")

;; `required-key` isn't built from an existing original predicate, but we add in 
;; a fake one just for completeness.
(def ^:private pretend-original (comp (not nil?)))

(def ^:private about-pred {:predicate-explainer explainer
                           :predicate-string pname
                           :predicate pretend-original})

(def required-key
  "False iff a key/path does not exist or has value `nil`. This is the only
   predicate that is not considered optional."
  (-> (fn [about-call]
        (let [{:keys [leaf-value] :as oopsie} (lifted/->oopsie about-pred about-call)]
          (if (nil? leaf-value)
            (e/left oopsie)
            (e/right (:leaf-value oopsie)))))
      lifted/mark-as-lifted
      (lifted/name-lifted-predicate pname)))
