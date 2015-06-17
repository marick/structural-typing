(ns structural-typing.frob
  "General-purpose functions for frobbing data in various ways.")

(defn update-each-value [kvs f & args]
  (reduce (fn [so-far k] 
            (assoc so-far k (apply f (get kvs k) args)))
          kvs
          (keys kvs)))


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
   a vectors with a path representing the existing nested structure. Keys that
   are already vectors terminate the descent. In the two-arg form, the resulting
   paths have the `parent-path` prepended"
  ([kvs]
     (nested-map->path-map [] kvs))
  ([parent-path kvs]
     (reduce (fn [so-far [k v]]
                 (cond (vector? k)
                       (assoc so-far (into parent-path k) v)

                       (map? v)
                       (merge so-far (nested-map->path-map (conj parent-path k) v))

                       :else
                       (assoc so-far (conj parent-path k) v)))
          {}
          kvs)))

