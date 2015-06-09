(ns structural-typing.type
  "Structural types, loosely inspired by Elm's way of looking at [records](http://elm-lang.org/learn/Records.elm).
   Builds on top of [Bouncer](https://github.com/leonardoborges/bouncer)."
  (:refer-clojure :exclude [instance?])
  (:require [clojure.string :as str]
            [bouncer.core :as b])
  (:require [structural-typing.pipeline-stages :as stages]
            [structural-typing.validators :as v]))

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
   :map-adapter stages/default-map-adapter
   :error-string-producer stages/default-error-string-producer
   })


(declare ^:private own-types)

;;; Non-side-effecting API

(declare checked)

(defn- update-each-value [kvs f & args]
  (reduce (fn [so-far k] 
            (assoc so-far k (apply f (get kvs k) args)))
          kvs
          (keys kvs)))


(defn- mkfn:catcher [f]
  (fn [& xs]
    (try (apply f xs)
    (catch Exception ex false))))

(defn- forgiving-optional-validator [descriptor]
  (let [almost
        (cond (var? descriptor)
              (with-meta (mkfn:catcher descriptor)
                {:default-message-format (format "%%s should be `%s`; it is `%%s`"
                                                 (:name (meta descriptor)))})

              (vector? descriptor)
              (let [[pred _key_ message] descriptor]
                (with-meta (mkfn:catcher pred)
                  {:default-message-format message}))

              :else 
              (with-meta (mkfn:catcher descriptor) (meta descriptor)))]

    (with-meta almost (assoc (meta almost) :optional true))))
  

(defn- tweaked-bouncer-descriptors [vec]
  (if-not (vector? vec)
    (tweaked-bouncer-descriptors (vector vec))
    (mapv forgiving-optional-validator vec)))

(defn- named-internal
  [type-repo name keys bouncer-map]
  (let [validator-map (reduce (fn [so-far k] (assoc so-far k [v/required]))
                              {}
                              keys)
        bouncer-map (update-each-value bouncer-map tweaked-bouncer-descriptors)]
    (assoc-in type-repo [:validators name]
              (merge-with into validator-map bouncer-map))))

(defn- checked-internal [type-repo name kvs]
  (let [[errors actual]
        (b/validate (:error-string-producer type-repo) kvs (get-in type-repo [:validators name]))]
    (if (empty? errors)
      ((:success-handler type-repo) kvs)
      (-> ( (:map-adapter type-repo) errors (dissoc actual :bouncer.core/errors))
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
     (checked @stages/global-type-repo name kvs)))

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
     (instance? @stages/global-type-repo name kvs)))

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
     (coerce @stages/global-type-repo name kvs)))


;;; Own types

(def ^:private own-types
  (-> empty-type-repo
      (assoc :failure-handler stages/throwing-failure-handler)
      (named-internal :type-repo [:success-handler :failure-handler :map-adapter] {})))


