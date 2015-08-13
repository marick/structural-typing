(ns structural-typing.global-type
  "If you want to mutate the state of a single global type repo, these functions support that."
  (:require [structural-typing.guts.type-repo :as repo]))


(def ^:no-doc repo (atom :no-value-yet))

(defn start-over!
  "Reset the global type repo to its starting state: no types defined, and default
   handling of error and success."
  []
  (reset! repo repo/empty-type-repo))
(start-over!)

(defn on-success!
  "Change the global type repo so that `f` is called when [[checked]]
   succeeds. `f` is given the original candidate structure. `f`'s return value becomes
   the return value of `checked`.
"
  [f]
  (swap! repo repo/replace-success-handler f))

(defn on-error!
  "Change the global type repo so that `f` is called when [[checked]]
   fails. `f` is given a list of [[oopsies]].
   `f`'s return value becomes the return value of `checked`.
"
  [f]
  (swap! repo repo/replace-error-handler f))

  
;; (defn coercion! 
;;   "Modify the global type repo to register function `f` as one that
;;    can coerce a map or record into one matching type `type-signifier`.
;;    See also [[coercion]]."
;;   [type-signifier f]
;;   (swap! stages/repo-type-repo type/coercion type-signifier f))

(defn type! 
  "Modifies the global type repo to define the type `type-signifier` as being
   a structure constrained by the `type-descriptions`.
   See also [[named]].
"
  [type-signifier & type-descriptions]
  (swap! repo repo/hold-type type-signifier type-descriptions))

