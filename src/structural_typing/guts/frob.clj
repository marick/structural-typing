(ns ^:no-doc structural-typing.guts.frob
  "General-purpose functions."
  (:require [such.immigration :as immigrate]))

(immigrate/import-vars [such.maps 
                         mkmap:all-keys-with-value
                         update-each-value]
                       [such.wrongness
                         boom!]
                       [such.types
                         extended-fn?]
                       [such.immigration import-vars])

(defn force-vector [v]
  (cond (vector? v) v
        (sequential? v) (vec v)
        :else (vector v)))

(defn adding-on [coll maybe-vector]
  (into coll (force-vector maybe-vector)))


