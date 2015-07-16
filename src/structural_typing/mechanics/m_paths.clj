(ns ^:no-doc structural-typing.mechanics.m-paths
  (:require [structural-typing.mechanics.frob :as frob]
            [such.function-makers :as mkfn]
            [com.rpl.specter :as specter]))

(def type-finder-key ::type-finder)

(defn type-finder? [x]
  (= type-finder-key (type x)))

(defn ends-in-map? [x]
  (cond (map? x)
        false
        
        (not (some map? x))
        false
        
        (map? (first x))
        (frob/boom! "A map cannot be the first element of a path: `%s`" x)
        
        (not (map? (last x)))
        (frob/boom! "Nothing may follow a map within a path: `%s`" x)
        
        :else
        true))

(defn element-will-match-many? [elt]
  (boolean (#{specter/ALL} elt)))

(defn path-will-match-many? [path]
  (boolean (some element-will-match-many? path)))

(defn replacement-points [path]
  (->> path
       (map vector (range))
       (filter (comp element-will-match-many? second))
       (map first)))

(def indexed (partial map-indexed vector))

(defn index-collecting-splice [elt]
  (vector (specter/view indexed)
          elt
          (specter/collect-one specter/FIRST)
          specter/LAST))

(def force-collection-of-indices
  (mkfn/lazyseq:x->abc index-collecting-splice element-will-match-many?))

(defn replace-with-indices [path replacement-points indices]
  (assert (= (count replacement-points) (count indices)))
  (loop [result path
         [r & rs] replacement-points
         [i & is] indices]
    (if r 
      (recur (assoc result r i) rs is)
      result)))
