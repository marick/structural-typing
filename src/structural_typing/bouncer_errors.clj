(ns structural-typing.bouncer-errors
  "Functions for working with bouncer errors. Especially converting them to
   a form more convenient to our purposes."
  (:require [structural-typing.frob :as frob]))

(defmulti simplify (fn [bouncer-value] (if (:metadata bouncer-value)
                                         :bouncer-data-for-message
                                         :bouncer-error-data)))


(defmethod simplify :bouncer-data-for-message [{path :path,
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

  











(defn flatten-error-map
  "`error-map` is a map from keys to either a sequence of error messages or
   a nested error map. This function reduces it to a sequence of error messages."
  [error-map]
  (mapcat #(if (map? %) (flatten-error-map %) %) (vals error-map)))



(defn prepend-bouncer-result-path [vals bouncer-result]
  (letfn [(update-path [kvs]
            (update-in kvs [:path] (fn [path] (into [] (concat vals path)))))
          (update-path-containers [v]
            (mapv update-path v))
          (update-result-map [kvs]
            (frob/update-each-value kvs update-path-containers))]

    (vector (update-result-map (first bouncer-result))
            (update-in (second bouncer-result) [:bouncer.core/errors] update-result-map))))


