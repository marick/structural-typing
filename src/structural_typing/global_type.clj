(ns structural-typing.global-type
  (:require [structural-typing.pipeline-stages :as stages]
            [structural-typing.type :as type]))

(defn start-over!
  "Reset the global type repo to its starting state: no types defined, and default
   handling of failure and success."
  []
  (alter-var-root #'stages/global-type-repo (fn [_] (atom type/empty-type-repo))))
(start-over!)

(defn set-success-handler!
  "Change the global type repo so that `f` is called when [[checked]]
   succeeds. `f` is given the original map or record. `f`'s return value becomes
   the return value of `checked`.
"
  [f]
  (swap! stages/global-type-repo assoc :success-handler f))

(defn set-failure-handler!
  "Change the global type repo so that `f` is called when [[checked]]
   fails. `f` is given a list of error messages. `f`'s return value becomes
   the return value of `checked`.
"
  [f]
  (swap! stages/global-type-repo assoc :failure-handler f))

(defn set-map-adapter!
  "If a map or record doesn't match the type, the formatter is called with 
   two arguments. The first is a map from key to list of error messages
   (as created by [Bouncer](https://github.com/leonardoborges/bouncer)).
   The second is the original map or record passed to `checked`."
  [f]
  (swap! stages/global-type-repo assoc :map-adapter f))
  
(defn coercion! 
  "Modify the global type repo to register function `f` as one that
   can coerce a map or record into one matching type `name`.
   See also [[coercion]]."
  [name f]
  (swap! stages/global-type-repo type/coercion name f))

(defn named! 
  "Modifies the global type repo to define the type `name` as being
   a map or record containing all of the given `keys`.
   See also [[named]].
"
  ([name keys]
     (named! name keys {}))
  ([name keys bouncer-map]
     (swap! stages/global-type-repo type/named name keys bouncer-map)))

