(ns structural-typing.util
  (:refer-clojure :exclude [instance?])
  (:require [clojure.string :as str]
            [bouncer.core :as b])
  (:require [structural-typing.pipeline-stages :as stages]
            [structural-typing.validators :as v]))

(defn update-each-value [kvs f & args]
  (reduce (fn [so-far k] 
            (assoc so-far k (apply f (get kvs k) args)))
          kvs
          (keys kvs)))


(defn mkfn:catcher [f]
  (fn [& xs]
    (try (apply f xs)
    (catch Exception ex false))))

(defn vector-or-wrap [vec-or-not]
  (if (vector? vec-or-not)
    vec-or-not
    (vector vec-or-not)))

(defn nested-map->path-map
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


(defn nested->paths
  ([v]
     (if (sequential? v)
       (nested->paths [[]] v)
       (vector v)))
       
  ([parent-paths v]
     (cond (empty? v)
           parent-paths

           (sequential? (first v))
           (let [extended (for [pp parent-paths, elt (first v)]
                            (conj pp elt))]
             (nested->paths (vec extended) (rest v)))

           :else
           (let [extended (for [pp parent-paths] (conj pp (first v)))]
             (nested->paths (vec extended) (rest v))))))

(defn expand-all-paths [v]
  (vec (mapcat nested->paths v)))

(defn var-message [v]
  (format "%%s should be `%s`; it is `%%s`" (:name (meta v))))



(defn prepend-bouncer-result-path [vals bouncer-result]
  (letfn [(update-path [kvs]
            (update-in kvs [:path] (fn [path] (into [] (concat vals path)))))
          (update-path-containers [v]
            (mapv update-path v))
          (update-result-map [kvs]
            (update-each-value kvs update-path-containers))]

    (vector (update-result-map (first bouncer-result))
            (update-in (second bouncer-result) [:bouncer.core/errors] update-result-map))))


