(ns structural-typing.bouncer-errors
  "Functions for working with bouncer errors. Especially converting them to
   a form more convenient to our purposes."
  (:require [structural-typing.frob :as frob]))

(defn fail? [bouncer-result]
  (boolean (first bouncer-result)))

(def pass? (complement fail?))

(def nested-explanation-map first)

(defn within-bouncer:simplify-raw-error-state
  "Bouncer provides a complicated data structure when it discovers an
   error. This function reduces that down to the most commonly-useful
   parts:
   
   * path: A list of keys to follow to the location of the error.
   * value: The value of the key in error. `nil` if the problem
     is that the key is missing.
   * predicate-args: Special predicate-makers take args when making a 
     predicate. Those args are included here. 
   * message: is either a format string or a function that is passed
     the original (pre-simplified) data. `message` is either (1)
     constructed from the predicate (when it's a var), (2) assigned
     with `validators/defvalidator`, or (3) overridden for a particular
     use of a predicate (with `type/message`). 

   Note that this function is called *before* `b/validate` returns.
"
  [{path :path,
    value :value
    predicate-args :args
    optional-message-arg :message
    {default-message-format :default-message-format
     predicate :validator} :metadata}]
  (let [handler (or optional-message-arg default-message-format
                    "configuration error: no message format. key %s val %s")]
    {:path path
     :value value
     :predicate-args predicate-args
     :message handler}))



(defn nested-explanation-map->explanations
  "`error-map` is a map from keys to either a sequence of error messages or
   a nested error map. This function reduces it to a sequence of error messages."
  [error-map]
  (mapcat #(if (map? %) (nested-explanation-map->explanations %) %) (vals error-map)))



(defn prepend-bouncer-result-path [vals bouncer-result]
  (letfn [(update-path [kvs]
            (update-in kvs [:path] (fn [path] (into [] (concat vals path)))))
          (update-path-containers [v]
            (mapv update-path v))
          (update-result-map [kvs]
            (frob/update-each-value kvs update-path-containers))]

    (vector (update-result-map (first bouncer-result))
            (update-in (second bouncer-result) [:bouncer.core/errors] update-result-map))))


