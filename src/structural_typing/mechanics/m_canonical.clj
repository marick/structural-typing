(ns ^:no-doc structural-typing.mechanics.m-canonical
  (:require [structural-typing.frob :as frob]
            [structural-typing.mechanics.ppps :as ppp]
            [structural-typing.api.path :as path]
            [structural-typing.api.predicates :as pred]
            [com.rpl.specter :as specter]
            [clojure.set :as set]))

(defn path-ending-in-map? [x]
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

;;; Decompressers undo one or more types of compression allowed in compressed type descriptions.

(defn dc:expand-type-signifiers [type-map form]
  (let [do-one #(if (path/type-finder? %) (% type-map) %)]
    (specter/update (specter/walker path/type-finder?) do-one form)))

(def dc:validate-starting-descriptions
  (frob/mkst:validator (some-fn map? sequential?)
                       #(frob/boom "Types are described with maps or vectors: `%s` has `%s`"
                                   %1 %2)))
(def dc:spread-collections-of-required-paths
  (frob/mkst:x->abc (partial map frob/force-vector) (complement map?)))

(def dc:split-paths-ending-in-maps
  (frob/mkst:x->abc #(let [prefix-path (pop %)]
                       (vector prefix-path (hash-map prefix-path (last %))))
                    path-ending-in-map?))
                    
(def dc:flatten-maps
  (letfn [(do-one [kvs parent-path]
            (reduce (fn [so-far [path v]]
                      (when (and (sequential? path)
                                 (some map? path))
                        (frob/boom "A path used as a map key may not itself contain a map: `%s`" path))
                      (let [extended-path (frob/adding-on parent-path path)]
                        (merge-with into so-far
                                    (if (map? v)
                                      (do-one v extended-path)
                                      (hash-map extended-path (frob/force-vector v))))))
                    {}
                    kvs))]
    (frob/mkst:x->y #(do-one % []) map?)))

(def dc:required-paths->maps 
  (frob/mkst:x->y #(hash-map % [pred/required-key]) (complement map?)))

(def forked-path? (partial some sequential?))

      
(defn dc:unfork-map-paths [maps]
  (map (fn [kvs]
         (reduce (fn [so-far [path v]]
                   (merge-with into so-far
                               (if (forked-path? path)
                                 (frob/mkmap:all-keys-with-value (structural-typing.mechanics.deriving-paths/from-forked-paths path) v)
                                 (hash-map path v))))
                 {}
                 kvs))
       maps))




(defn canonicalize [type-map & condensed-type-descriptions]
  (when (empty? condensed-type-descriptions)
    (frob/boom "Canonicalize was called with no type descriptions. Type-map: %s" type-map))

  (->> condensed-type-descriptions
       (dc:expand-type-signifiers type-map) ; comes first because signifiers can be top-level
       dc:validate-starting-descriptions

       ;; Let's work with the vectors of required paths, ending up with maps
       dc:spread-collections-of-required-paths      ; affects vectors, skips maps
       dc:split-paths-ending-in-maps   ; produces new vectors and maps
       dc:required-paths->maps         ; everything is now a flatmap w/ potentially forking keys

       dc:flatten-maps

       ppp/flatmaps->ppps
       ppp/fix-forked-paths
       ppp/fix-required-paths-with-collection-selectors
       ppp/dc2:ppps->type-description))




