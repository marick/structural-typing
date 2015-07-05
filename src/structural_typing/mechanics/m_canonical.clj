(ns ^:no-doc structural-typing.mechanics.m-canonical
  (:require [structural-typing.frob :as frob]
            [structural-typing.mechanics.ppps :as ppp]
            [structural-typing.api.path :as path]
            [structural-typing.api.predicates :as pred]
            [com.rpl.specter :as specter]
            [clojure.set :as set]))


;;; Utilities 












(defn- mkfn:passing-through [ignore-pred process-fn]
  (fn [mixture]
    (map #(if (ignore-pred %)
            %
            (process-fn %))
         mixture)))
  
;;; Decompressers undo one or more types of compression allowed in compressed type descriptions.

(defn dc:validate-description-types [mixture]
  ( (mkfn:passing-through (some-fn map? sequential?)
                          #(frob/boom "Types are described with maps or vectors: `%s` has `%s`"
                                      mixture %))
    mixture))

(defn dc:expand-type-signifiers [type-map form]
  (let [do-one #(if (path/type-finder? %) (% type-map) %)]
    (specter/update (specter/walker path/type-finder?) do-one form)))


(def dc:spread-collections-of-required-paths
  (frob/mkst:x->abc (partial map frob/force-vector) (complement map?)))


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

(def dc:split-paths-ending-in-maps
  (frob/mkst:x->abc #(let [prefix-path (pop %)]
                       (vector prefix-path (hash-map prefix-path (last %))))
                    path-ending-in-map?))
                    

(defn dc:flatten-maps [mixture]
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
    ( (mkfn:passing-through sequential? #(do-one % [])) mixture)))

(def dc:required-paths->maps 
  (mkfn:passing-through map? #(hash-map % [pred/required-key])))

(defn dc:validate-all-are-flatmaps [xs]
  (doseq [x xs]
    (when-not (map? x)
      (frob/boom "Program error: %s should be a map" x))
    (doseq [[k v] x]
      (when-not (vector? k)
        (frob/boom "Program error: %s is supposed to be an expanded path" x))
      
      (when-not (vector? v)
        (frob/boom "Program error: %s is supposed to be a vector of predicates" x))))
  xs)

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
       (dc:expand-type-signifiers type-map)
       dc:validate-description-types
       dc:spread-collections-of-required-paths      ; affects vectors, skips maps
       dc:split-paths-ending-in-maps   ; produces new vectors and maps
       dc:flatten-maps                 ; affects maps
       dc:required-paths->maps         ; everything is now a flatmap w/ potentially forking keys
       dc:validate-all-are-flatmaps
       dc:unfork-map-paths
       (apply merge-with into)
;       dc:fix-required-paths-with-collection-selectors
       ))




(defn canonicalize2 [type-map & condensed-type-descriptions]
  (when (empty? condensed-type-descriptions)
    (frob/boom "Canonicalize was called with no type descriptions. Type-map: %s" type-map))

  (->> condensed-type-descriptions
       (dc:expand-type-signifiers type-map)
       dc:validate-description-types
       dc:spread-collections-of-required-paths      ; affects vectors, skips maps
       dc:split-paths-ending-in-maps   ; produces new vectors and maps
       dc:flatten-maps                 ; affects maps
       dc:required-paths->maps         ; everything is now a flatmap w/ potentially forking keys
       dc:validate-all-are-flatmaps
       dc:unfork-map-paths
       (apply merge-with into)
;       dc:fix-required-paths-with-collection-selectors
       ))





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dc2:signifier-free->condensed-maps [[x & xs :as stream]]
)

(defn dc2:condensed-maps->condensed-ppps [[x & xs :as stream]]
)


(defn dc2:condensed-ppps->ppps [[x & xs :as stream]]
  (-> stream
      ppp/fix-forked-paths))


