(ns ^:no-doc structural-typing.mechanics.m-canonical
  (:require [structural-typing.frob :as frob]
            [structural-typing.api.path :as path]
            [structural-typing.api.predicates :as pred]
            [com.rpl.specter :as specter]))


;;; Utilities 


(def forked-path? (partial some sequential?))

(defn flatten-forked-path
  "Expand a vector containing path elements + shorthand for forks into 
   a vector of paths"
  ([path]
     (flatten-forked-path path [[]]))
       
  ([[x & xs :as path] parent-paths]
     (cond (empty? path)
           parent-paths
           
           (sequential? x)
           (let [extended (for [pp parent-paths, elt x]
                            (conj pp elt))]
             (flatten-forked-path xs (vec extended)))
           
           (map? x)
           (frob/boom "Program error: Path contains a map: %s." path)
           
           :else
           (let [extended (for [pp parent-paths] (conj pp x))]
             (flatten-forked-path xs (frob/force-vector extended))))))

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


(defn dc:spread-path-collections
  "Take user arguments and convert vectors by splicing in their components"
  [condensed]
  (reduce (fn [so-far one]
            (if (map? one)
              (conj so-far one)
              (into so-far (map frob/force-vector one))))
          []
          condensed))

(defn dc:split-paths-ending-in-maps [condensed]
  ;; assertion: dc:spread-path-collections has already been called
  (loop [[x & xs] condensed
         result []]
    (cond (nil? x)
          result

          (map? x)
          (recur xs (conj result x))

          (not (some map? x))
          (recur xs (conj result x))

          (map? (first x))
          (frob/boom "A map cannot be the first element of a path: `%s`" x)
                        
          (not (map? (last x)))
          (frob/boom "Nothing may follow a map within a path: `%s`" x)
                        
          :else
          (let [prefix-path (pop x)]
            (recur xs (into result (vector prefix-path (hash-map prefix-path (last x)))))))))

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
      
(defn dc:unfork-map-paths [maps]
  (map (fn [kvs]
         (reduce (fn [so-far [path v]]
                   (merge-with into so-far
                               (if (forked-path? path)
                                 (frob/mkmap:all-keys-with-value (flatten-forked-path path) v)
                                 (hash-map path v))))
                 {}
                 kvs))
       maps))

(defn dc:add-required-subpaths [kvs]
  (letfn [(candidate-preds? [preds]
            (some #{pred/required-key} preds))
          (split-paths [path]
            (reduce (fn [so-far [prefix current]]
                      (cond (keyword? current)
                            so-far

                            ;; This is the [... ALL ALL ...] case
                            (not (keyword? (last prefix)))
                            so-far

                            :else 
                            (conj so-far prefix)))
                    []
                    (map vector (reductions conj [] path) path)))]

    (let [ensure-required (->> kvs
                               (map identity)
                               (filter #(candidate-preds? (second %)))
                               (map first)
                               (mapcat split-paths))]
      (reduce (fn [so-far ensure]
                (cond (not (contains? so-far ensure))
                      (assoc so-far ensure [pred/required-key])
                      
                      (some #{pred/required-key} (so-far ensure))
                      so-far
                      
                      :else 
                      (update-in so-far [ensure] conj pred/required-key)))
              kvs
              ensure-required))))


(defn canonicalize [type-map & condensed-type-descriptions]
  (when (empty? condensed-type-descriptions)
    (frob/boom "Canonicalize was called with no type descriptions. Type-map: %s" type-map))

  (->> condensed-type-descriptions
       (dc:expand-type-signifiers type-map)
       dc:validate-description-types
       dc:spread-path-collections      ; affects vectors, skips maps
       dc:split-paths-ending-in-maps   ; produces new vectors and maps
       dc:flatten-maps                 ; affects maps
       dc:required-paths->maps         ; everything is now a flatmap w/ potentially forking keys
       dc:validate-all-are-flatmaps
       dc:unfork-map-paths
       (apply merge-with into)
;       dc:add-required-subpaths
       ))
