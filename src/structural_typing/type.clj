(ns structural-typing.type
  "Structural types, loosely inspired by Elm's way of looking at [records](http://elm-lang.org/learn/Records.elm).
   Builds on top of [Bouncer](https://github.com/leonardoborges/bouncer)."
  (:refer-clojure :exclude [instance?])
  (:require [bouncer.core :as b])
  (:require [structural-typing.pipeline-stages :as stages]
            [structural-typing.validators :as v]
            [structural-typing.frob :as frob]
            [structural-typing.bouncer-validators :as b-in]
            [structural-typing.bouncer-errors :as b-err]))


(def ^:private error-stream-kludge (atom []))

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
   :error-explanation-producer stages/default-error-explanation-producer
   })

(declare ^:private own-types)

(defn message
  "Modify `pred` so that the given `message` is used to construct an error
   message. `message` may be a string, in which case it takes up to two 
   format descriptors, stringified versions of the key and the offending
   value:
   
        (-> even? (type/message \"Bad key %s with value %s\"))
   
   The `message` may also be a function. In that case, it takes a map with a
   `:path` (a vector of keys leading to a value in a possibly nested map)
   and a `:value`.
"
  [pred message]
  (with-meta pred (assoc (meta pred) :default-message-format message)))

(defn only-when [pred precondition]
  (let [guarded (with-meta #(if (precondition %) (pred %) true) (meta pred))]
    ;; TODO: Icky that varness is checked in two places.
    (if (var? pred)
      (-> guarded (message (stages/var-message pred)))
      guarded)))

(defn- forgiving-optional-validator [descriptor]
  (let [almost
        (cond (var? descriptor)
              (let [msg (stages/var-message descriptor)]
                (-> descriptor frob/wrap-pred-with-catcher (message msg)))

              ;; TODO: Bouncer backwards compatibility - delete?
              (and (vector? descriptor) (= :message (second descriptor)))
              (let [[pred _key_ msg] descriptor]
                (println "Use `(-> pred (type/message ...))` in preference to `[pred :message msg]`")
                (-> pred frob/wrap-pred-with-catcher (message msg)))

              :else 
              (with-meta (frob/wrap-pred-with-catcher descriptor) (meta descriptor)))]
    (with-meta almost (assoc (meta almost) :optional true))))
  

(defn- expanded-optional-value-descriptor [v]
  (mapv forgiving-optional-validator (frob/force-vector v)))

(defn- named-internal
  [type-repo type-signifier paths optional-map]
  (let [validator-map (reduce (fn [so-far k] (assoc so-far k [v/required]))
                              {}
                              (b-in/flatten-N-path-representations paths))
        optional-map (frob/update-each-value optional-map expanded-optional-value-descriptor)]
    (assoc-in type-repo [:validators type-signifier]
              (merge-with into validator-map optional-map))))

(defn- checked-internal [type-repo type-signifier candidate]
  (letfn [(run-validation []
            (b/validate (:error-explanation-producer type-repo) candidate (get-in type-repo [:validators type-signifier])))

          (run-error-handling [[errors actual]]
            (-> ( (:map-adapter type-repo) errors (dissoc actual :bouncer.core/errors))
                ((:failure-handler type-repo))))]

    (reset! error-stream-kludge [])
    (let [bouncer-result (run-validation)]
      (cond (empty? (first bouncer-result))
            ((:success-handler type-repo) candidate)
            
            (empty? @error-stream-kludge)
            (run-error-handling bouncer-result)
            
            :else 
            (do
              (let [error-stream @error-stream-kludge
                    prefix (-> bouncer-result first first first frob/force-vector)]
                (reset! error-stream-kludge [])
                (doseq [old-bouncer-result error-stream]
                  (run-error-handling (b-err/prepend-bouncer-result-path prefix old-bouncer-result)))))))))

(defn checked
  "Check the map `candidate` against the previously-defined type `type-signifier` in the given
   `type-repo`. If the `type-repo` is omitted, the global one is used.
   
       (type/checked :frobbish {:twerk true, :tweek false})
   
   Types are defined with [[named]] or [[named!]]. By default, `checked` returns
   the `candidate` argument if it checks out, `nil` otherwise. Those defaults can be
   changed.
"
  ([type-repo type-signifier candidate]
     (checked-internal own-types :type-repo type-repo)
     (checked-internal type-repo type-signifier candidate))
  ([type-signifier candidate]
     (checked @stages/global-type-repo type-signifier candidate)))


(defn named 
  "Define the type `type-signifier` as being a map or record containing all of the given `paths`.
  (A path is a key or a vector of paths.)
  Returns the augmented `type-repo`. See also [[named!]].
"
  ([type-repo type-signifier paths optional-map]
     (checked-internal own-types :type-repo type-repo)
     (named-internal type-repo type-signifier paths (b-in/nested-map->path-map optional-map)))
  ([type-repo type-signifier paths]
     (named type-repo type-signifier paths {})))

  
(defn instance? 
  "Return `true` iff the map or record `candidate` typechecks against the type named `type-signifier` in
   `type-repo`. If `type-repo` is omitted, the global repo is used.
   
       (type/instance? :frobbable candidate)
"
  ([type-repo type-signifier candidate]
     (checked (assoc type-repo
                     :failure-handler (constantly false) 
                     :success-handler (constantly true))
              type-signifier 
              candidate))
  ([type-signifier candidate]
     (instance? @stages/global-type-repo type-signifier candidate)))

(defn coercion 
  "Register function `f` as one that can coerce a map or record into 
   one that matches type `type-signifier`. The updated `type-repo` is returned.
   See also [[coercion!]], which updates the global type repo."
  [type-repo type-signifier f]
  (checked-internal own-types :type-repo type-repo)
  (assoc-in type-repo [:coercions type-signifier] f))

(defn coerced
  "Coerce the map or record `candidate` into the type named `type-signifier` in the `type-repo`
   and check the result with [[checked]]. The coerced version of `candidate` is returned.
   
   If the type repo is omitted, the global type repo is used.
   Coercions are defined with [[coercion]] or [[coercion!]].
   
        (some-> (coerce :user-v2 legacy-json)
                (update-in [:stats :logins] inc))

   If `type-signifier` hasn't been defined (via [[name]] or [[named!]]), the final
   call to `checked` is omitted.
"
  ([type-repo type-signifier candidate]
     (checked-internal own-types :type-repo type-repo)
     (let [coercer (get-in type-repo [:coercions type-signifier] identity)]
       (->> (coercer candidate)
            (checked-internal type-repo type-signifier))))
  ([type-signifier candidate]
     (coerced @stages/global-type-repo type-signifier candidate)))

(defn each-is
  ([type-repo type-signifier]
     (let [validator (get-in type-repo [:validators type-signifier])]
       (fn [xs]
         (let [erroneous 
               (loop [xs xs, i 0, erroneous []]
                 (if (empty? xs) 
                   erroneous
                   (let [result (b/validate identity (first xs) validator)]
                     (recur (next xs)
                            (inc i)
                            (if (nil? (first result))
                              erroneous
                              (conj erroneous (b-err/prepend-bouncer-result-path [i] result)))))))]
           (swap! error-stream-kludge into erroneous)
           (empty? erroneous)))))
  ([type-signifier]
     (each-is @stages/global-type-repo type-signifier)))
  

;;; Own types

(def ^:private own-types
  (-> empty-type-repo
      (assoc :failure-handler stages/throwing-failure-handler)
      (named-internal :type-repo [:success-handler :failure-handler :map-adapter] {})))


