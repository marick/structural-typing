(ns ^:no-doc structural-typing.guts.mechanics.m-maps
  (:use structural-typing.clojure.core))


(defn flatten-map
  "When path keys point to maps with path keys, make one-level map with concatenated paths."
  ([kvs parent-path]
     (reduce (fn [so-far [path v]]
               (when (and (sequential? path)
                          (some map? path))
                 (boom! "A path used as a map key may not itself contain a map: `%s`" path))
               (let [extended-path (adding-on parent-path path)]
                 (merge-with into so-far
                             (if (map? v)
                               (flatten-map v extended-path)
                               (hash-map extended-path (force-vector v))))))
             {}
             kvs))
  ([kvs]
     (flatten-map kvs [])))

