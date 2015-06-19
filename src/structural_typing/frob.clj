(ns structural-typing.frob
  "General-purpose functions for frobbing data in various ways.")

(defn boom [fmt & args]
  (throw (new RuntimeException (apply format fmt args))))

(defn update-each-value [kvs f & args]
  (reduce (fn [so-far k] 
            (assoc so-far k (apply f (get kvs k) args)))
          kvs
          (keys kvs)))

(defn mkmap:all-keys-with-value [keys v]
  (reduce (fn [so-far k]
            (assoc so-far k v))
          {}
          keys))


(defn wrap-pred-with-catcher [f]
  (fn [& xs]
    (try (apply f xs)
    (catch Exception ex false))))

(defn force-vector [v]
  (cond (vector? v) v
        (sequential? v) (vec v)
        :else (vector v)))

(defn nested-map->path-map
  "In single-argument form, converts a nested map into a flat one where the keys
   a vectors with a path representing the existing nested structure. 
   In the two-arg form, the resulting paths have the `parent-path` prepended.
   Note that vector keys are considered to come from an already-flattened
   map. That is, they are spliced into the resulting path.

   Non-vector leaf values are converted into vectors."
  ([kvs]
     (nested-map->path-map [] kvs))
  ([parent-path kvs]
     (letfn [(splice [maybe-vector]
               (into parent-path (force-vector maybe-vector)))]
       (reduce (fn [so-far [k v]]
                 (if (map? v)
                 (merge so-far (nested-map->path-map (splice k) v))
                 (assoc so-far (splice k) (force-vector v))))
               {}
               kvs))))



(defn flatten-path-representation
  "Convert an atom into a vector of that sequential.
   Convert a sequential into a flattened vector.
   A vector with a subvector of length N produces N flattened vectors.
   Maps are converted to a vector of their keys."
  ([v]
     (cond (sequential? v)
           (flatten-path-representation [[]] v)

           (map? v)
           (into [] (keys (nested-map->path-map v)))
           
           :else
           (vector v)))
       
  ([parent-paths v]
     (cond (empty? v)
           parent-paths

           (sequential? (first v))
           (let [extended (for [pp parent-paths, elt (first v)]
                            (conj pp elt))]
             (flatten-path-representation (vec extended) (rest v)))

           (map? (first v))
           (if (> (count v) 1)
             (throw (RuntimeException. (format "The map must be the last element of the vector: %s." v)))
             (for [pp parent-paths, elt (keys (nested-map->path-map (first v)))]
                 (into pp (force-vector elt))))

           :else
           (let [extended (for [pp parent-paths] (conj pp (first v)))]
             (flatten-path-representation (vec extended) (rest v))))))

(defn flatten-N-path-representations [v]
  (vec (mapcat flatten-path-representation v)))
