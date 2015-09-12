(ns structural-typing.guts.type-descriptions.flatten
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.type-descriptions.m-maps :as m-map]
            [structural-typing.guts.type-descriptions.dc-type-maps :as dc-type-map]))

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
      (handle-kvs (m-map/flatten-map arg))
      (-> (fn [type-map]
            (handle-kvs ( (dc-type-map/includes arg) type-map)))
          dc-type-map/as-type-expander))))
