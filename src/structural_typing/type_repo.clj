(ns structural-typing.type-repo
  (:require [structural-typing.frob :as frob]
            [structural-typing.predicates :as p]))

(defn canonicalize [& signifiers]
  (letfn [(handle-one [signifier]
            (cond (map? signifier)
                  (frob/nested-map->path-map signifier)

                  (vector? signifier)
                  (canonicalize (reduce (fn [so-far k] (assoc so-far k p/must-exist))
                                        {}
                                        (frob/flatten-N-path-representations signifier)))

                  :else
                  (throw (ex-info "A type signifier must be a map or vector" {:actual signifier}))))]
    (apply merge-with into (map handle-one signifiers))))
  
  
