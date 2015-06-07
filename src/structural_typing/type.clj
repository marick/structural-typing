(ns structural-typing.type
  "Structural types, loosely inspired by Elm's way of looking at [records](http://elm-lang.org/learn/Records.elm).
   Builds on top of [Bouncer](https://github.com/leonardoborges/bouncer)."
  (:refer-clojure :exclude [instance?])
  (:require [clojure.string :as str]
            [bouncer.core :as b])
  (:require [structural-typing.pipeline-stages :as stages]
            [structural-typing.validators :as v]))

;;; About our type

(def empty-type-repo
  "A repository that contains no type descriptions. It contains
   default behavior for both success and failure cases. Here's
   an example of changing the behavior and adding a type:
   
         (-> type/empty-type-repo
             (assoc :failure-handler type/throwing-failure-handler)
             (type/named :frobable [:left :right :arrow]))
"
  {:failure-handler stages/default-failure-handler
   :success-handler stages/default-success-handler
   :bouncer-map-adapter stages/default-bouncer-map-adapter
   :error-string-producer stages/default-error-string-producer
   })

(declare ^:private own-types)

;;; Non-side-effecting API

(declare checked global-type-repo)

(defn- update-each-value [kvs f & args]
  (reduce (fn [so-far k] 
            (assoc so-far k (apply f (get kvs k) args)))
          kvs
          (keys kvs)))

(defn- named-internal
  [type-repo name keys bouncer-map]
  (let [validator-map (reduce (fn [so-far k] (assoc so-far k [v/required]))
                              {}
                              keys)
        bouncer-map (update-each-value bouncer-map #(if (vector? %) % (vector %)))]
    (assoc-in type-repo [:validators name]
              (merge-with into validator-map bouncer-map))))

(defn- checked-internal [type-repo name kvs]
  (let [[errors actual]
        (b/validate (:error-string-producer type-repo) kvs (get-in type-repo [:validators name]))]
    (if (empty? errors)
      ((:success-handler type-repo) kvs)
      (-> ( (:bouncer-map-adapter type-repo) errors (dissoc actual :bouncer.core/errors))
          ((:failure-handler type-repo))))))

(defn named 
  "Define the type `name` as being a map or record containing all of the given `keys`.
   Returns the augmented `type-repo`. See also [[named!]].
"
  ([type-repo name keys bouncer-map]
     (checked-internal own-types :type-repo type-repo)
     (named-internal type-repo name keys bouncer-map))
  ([type-repo name keys]
     (named type-repo name keys {})))

(defn optional
  "Construct a function that takes a single value.

   * If that value is `nil`, the result is `true`.
   * Otherwise, if the value causes all of the `fns` to return a truthy value, the result is `true`.
   * Otherwise, the result is `false`.

       ( (type/optional number? even?) nil) => true
       ( (type/optional number? even?) 'a) => false  ; note evaluation short-circuits.
       ( (type/optional number? even?) 1) => false
       ( (type/optional number? even?) 2) => true"
  [& fns]
  (fn [value]
    (if (nil? value)
      true
      (try 
        ( (apply every-pred fns) value)
        (catch Exception ex false)))))
  
(defn checked
  "Check the map `kvs` against the previously-defined type `name` in the given
   `type-repo`. If the `type-repo` is omitted, the global one is used.
   
       (type/checked :frobbish {:twerk true, :tweek false})
   
   Types are defined with [[named]] or [[named!]]. By default, `checked` returns
   the `kvs` argument if it checks out, `nil` otherwise. Those defaults can be
   changed.
"
  ([type-repo name kvs]
     (checked-internal own-types :type-repo type-repo)
     (checked-internal type-repo name kvs))
  ([name kvs]
     (checked @global-type-repo name kvs)))

(defn instance? 
  "Return `true` iff the map or record `kvs` typechecks against the type named `name` in
   `type-repo`. If `type-repo` is omitted, the global repo is used.
   
       (type/instance? :frobbable kvs)
"
  ([type-repo name kvs]
     (checked (assoc type-repo
                     :failure-handler (constantly false) 
                     :success-handler (constantly true))
              name 
              kvs))
  ([name kvs]
     (instance? @global-type-repo name kvs)))

(defn coercion 
  "Register function `f` as one that can coerce a map or record into 
   one that matches type `name`. The updated `type-repo` is returned.
   See also [[coercion!]], which updates the global type repo."
  [type-repo name f]
  (checked-internal own-types :type-repo type-repo)
  (assoc-in type-repo [:coercions name] f))

(defn coerce
  "Coerce the map or record `kvs` into the type named `name` in the `type-repo`
   and check the result with [[checked]]. The coerced version of `kvs` is returned.
   
   If the type repo is omitted, the global type repo is used.
   Coercions are defined with [[coercion]] or [[coercion!]].
   
        (some-> (coerce :user-v2 legacy-json)
                (update-in [:stats :logins] inc))

   If `name` hasn't been defined (via [[name]] or [[named!]]), the final
   call to `checked` is omitted.
"
  ([type-repo name kvs]
     (checked-internal own-types :type-repo type-repo)
     (let [coercer (get-in type-repo [:coercions name] identity)]
       (->> (coercer kvs)
            (checked-internal type-repo name))))
  ([name kvs]
     (coerce @global-type-repo name kvs)))


;;; Own types

(def ^:private own-types
  (-> empty-type-repo
      (assoc :failure-handler stages/throwing-failure-handler)
      (named-internal :type-repo [:success-handler :failure-handler :bouncer-map-adapter] {})))

;;; Side-effecting API

(def ^:private global-type-repo)

(defn start-over!
  "Reset the global type repo to its starting state: no types defined, and default
   handling of failure and success."
  []
  (alter-var-root #'global-type-repo (fn [_] (atom empty-type-repo))))
(start-over!)

(defn set-success-handler!
  "Change the global type repo so that `f` is called when [[checked]]
   succeeds. `f` is given the original map or record. `f`'s return value becomes
   the return value of `checked`.
"
  [f]
  (swap! global-type-repo assoc :success-handler f))

(defn set-failure-handler!
  "Change the global type repo so that `f` is called when [[checked]]
   fails. `f` is given a list of error messages. `f`'s return value becomes
   the return value of `checked`.
"
  [f]
  (swap! global-type-repo assoc :failure-handler f))

;; This will be relevant once more of bouncer is exposed.
(defn set-formatter!
  "If a map or record doesn't match the type, the formatter is called with 
   two arguments. The first is a map from key to list of error messages
   (as created by [Bouncer](https://github.com/leonardoborges/bouncer)).
   The second is the original map or record passed to `checked`."
  [f]
  (swap! global-type-repo assoc :bouncer-map-adapter f))
  
(defn named! 
  "Modifies the global type repo to define the type `name` as being
   a map or record containing all of the given `keys`.
   See also [[named]].
"
  [name keys]
  (swap! global-type-repo named name keys))

(defn coercion! 
  "Modify the global type repo to register function `f` as one that
   can coerce a map or record into one matching type `name`.
   See also [[coercion]]."
  [name f]
  (swap! global-type-repo coercion name f))

