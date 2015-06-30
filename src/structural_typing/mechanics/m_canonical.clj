(ns ^:no-doc structural-typing.mechanics.m-canonical
  (:require [structural-typing.frob :as frob]
            [structural-typing.api.path :as path]
            [structural-typing.api.predicates :as pred]
            [com.rpl.specter :as specter]))

(defn force-literal [v type-map]
  (if (path/type-finder? v) (v type-map) v))

(defn expand-type-finders [type-map form]
  (specter/update (specter/walker path/type-finder?)
                  #(force-literal % type-map)
                  form))

(defn undo-singleton-path-convenience [x]
  (if (sequential? x) x (vector x)))

(declare expand-path-with-forks)

(defn validate-description [description]
  (when-not (or (map? description)
                (sequential? description))
    (frob/boom "Types are described with maps or vectors: %s" description))
  description)

(defn any-required-seq->maps [description]
  (cond (map? description)
        description
        
        (sequential? description)
        (frob/mkmap:all-keys-with-value
         (mapcat expand-path-with-forks (map undo-singleton-path-convenience description))
         (vector pred/required-key))
        
        :else
        (frob/boom "Unexpected type description: %s" description)))

(defn- splice [parent-path maybe-vector]
  (into parent-path (frob/force-vector maybe-vector)))

(defn nested-map->path-map
  ([kvs parent-path]
     (reduce (fn [so-far [possibly-forking-path-element v]]
               (reduce (fn [so-far flat-element]
                         (let [additions (if (map? v)
                                           (nested-map->path-map v (splice parent-path flat-element))
                                           (hash-map (splice parent-path flat-element)
                                                     (frob/force-vector v)))]
                           (merge-with into so-far additions)))
                       so-far
                       (expand-path-with-forks (frob/force-vector possibly-forking-path-element))))
             {}
             kvs))
  ([kvs]
     (nested-map->path-map kvs [])))

(defn expand-path-with-forks
  "Expand a vector containing path elements + shorthand for forks into 
   a vector of paths"
  ([value]
     (expand-path-with-forks value [[]]))
       
  ([[x & xs :as value] parent-paths]
     (let [x (first value)
           xs (rest value)]
       (cond (empty? value)
             parent-paths

             (sequential? x)
             (let [extended (for [pp parent-paths, elt x]
                              (conj pp elt))]
               (expand-path-with-forks xs (vec extended)))
             
             (and (map? x) (empty? xs))
             (for [pp parent-paths, elt (keys (nested-map->path-map x []))]
               (into pp (frob/force-vector elt)))

             (map? x)
             (frob/boom "The map must be the last element of the vector: %s." value)
             
             :else
             (let [extended (for [pp parent-paths] (conj pp x))]
               (expand-path-with-forks xs (frob/force-vector extended)))))))

  
(defn canonicalize [type-map & condensed-type-descriptions]
  (when (empty? condensed-type-descriptions)
    (frob/boom "Canonicalize was called with no type descriptions. Type-map: %s" type-map))
  (->> condensed-type-descriptions
       (expand-type-finders type-map)
       (map validate-description)
       (map any-required-seq->maps)
       (map nested-map->path-map)
       (apply merge-with into)))
