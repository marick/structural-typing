(ns structural-typing.guts.type-descriptions.flatten
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.type-descriptions.dc-type-maps :as dc-type-map]
            [such.sequences :as seq]))

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

(declare map->flatmap map->pairs)

(defn- step1:expand-map-values [kvs parent-path]
  (reduce (fn [so-far [path v]]
            (let [extended-path (adding-on parent-path path)]
              (into so-far (if (map? v)
                             (step1:expand-map-values v extended-path)
                             [(vector extended-path (force-vector v))]))))
          []
          kvs))

(defn- step2:flatten-paths [pairs]
  (reduce (fn [so-far [maybe-forked-path v]]
            (->> (uncondense-path maybe-forked-path)
                 (map #(vector % v))
                 (into so-far)))
          []
          pairs))

(defn- step3:expand-maps-in-pred-list [pairs]
  (reduce (fn [so-far [path preds]]
            (let [[maps plain] (seq/bifurcate map? preds)
                  one-plain-pair (if (empty? plain) [] [(vector path plain)])
                  n-map-pairs (mapcat #(map->pairs % path) maps)]
              (-> so-far
                  (into n-map-pairs)
                  (into one-plain-pair))))
          []
          pairs))

(defn map->pairs [kvs parent-path]
  (-> kvs
      (step1:expand-map-values parent-path)
      step2:flatten-paths
      step3:expand-maps-in-pred-list))

(defn map->flatmap
  ([kvs parent-path]
     (reduce (fn [so-far [k v]]
               (merge-with into so-far (hash-map k v)))
             {}
             (map->pairs kvs parent-path)))
  ([kvs]
     (map->flatmap kvs [])))
