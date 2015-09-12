(ns structural-typing.guts.type-descriptions.flatten
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.type-descriptions.dc-type-maps :as dc-type-map]))

(declare map->flatmap)

;;; Flattening paths


(defprotocol CondensedPath
  (->paths [this]))

(defprotocol PathComponent
  (add-on [this paths]))


(extend-type clojure.lang.Keyword
  PathComponent
  (add-on [this paths]
    (map #(conj % this) paths)))

(extend-type clojure.lang.PersistentVector
  CondensedPath
  (->paths [this]
    (reduce (fn [so-far elt] (add-on elt so-far))
            [[]]
            this)))

(defrecord Fork [alternatives]
  CondensedPath
  (->paths [this] (add-on this [[]]))
  PathComponent
  (add-on [this paths]
    (let [addons (mapcat ->paths alternatives)]
      (for [path paths
            addon addons]
        (into path addon)))))

(defn through-each [& alternatives]
  (->Fork (map force-vector alternatives)))
(def each-of through-each)

(defn paths-of [arg]
  (let [handle-kvs #(->Fork (keys %))]
    (if (map? arg)
      (handle-kvs (map->flatmap arg))
      (-> (fn [type-map]
            (handle-kvs ( (dc-type-map/includes arg) type-map)))
          dc-type-map/as-type-expander))))


(defn uncondense-path [condensed-path]
  (->paths condensed-path))


;;;; Flattening maps

(defn map->flatmap
  "When path keys point to maps with path keys, make one-level map with concatenated paths."
  ([kvs parent-path]
     (reduce (fn [so-far [path v]]
               (let [extended-path (adding-on parent-path path)]
                 (merge-with into so-far
                             (if (map? v)
                               (map->flatmap v extended-path)
                               (hash-map extended-path (force-vector v))))))
             {}
             kvs))
  ([kvs]
     (map->flatmap kvs [])))

