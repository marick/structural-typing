(ns ^:no-doc structural-typing.mechanics.m-paths
  (:require [structural-typing.frob :as frob]))

(def type-finder-key ::type-finder)

(defn type-finder? [x]
  (= type-finder-key (type x)))

(defn ends-in-map? [x]
  (cond (map? x)
        false
        
        (not (some map? x))
        false
        
        (map? (first x))
        (frob/boom "A map cannot be the first element of a path: `%s`" x)
        
        (not (map? (last x)))
        (frob/boom "Nothing may follow a map within a path: `%s`" x)
        
        :else
        true))


