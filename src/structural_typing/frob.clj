(ns ^:no-doc structural-typing.frob
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

(defn adding-on [coll maybe-vector]
  (into coll (force-vector maybe-vector)))


