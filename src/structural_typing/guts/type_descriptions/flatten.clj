(ns ^:no-doc structural-typing.guts.type-descriptions.flatten
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.type-descriptions.includes :as includes]
            [com.rpl.specter :as specter]))

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

(defn through-each
  "Use `through-each` to describe a \"forking\" path. This is convenient when two
   parts of a bigger data structure should be built the same way.
   
       (type! :Line {[(through-each :start :end) :location] (includes :Point)})
   
   [[each-of]] is a synonym. I tend to use `each-of` for the end of the path,
   `through-each` for a fork earlier than that."
  [& alternatives]
  (->Fork (map force-vector alternatives)))
(def each-of
  "Use `each-of` to describe a \"forking\" path. This is convenient when two
   parts of a bigger data structure should be built the same way.
   
       (type! :Plat {[:corners (each-of :nw :ne :sw :se)] (includes :GeoPoint)})
   
   [[through-each]] is a synonym. I tend to use `each-of` for the end of the path,
   `through-each` for a fork earlier than that."
  through-each)

(defn paths-of
  "Include all the paths of a type (or a literal map) within a path.
   
        (type! :StrictX (includes :X)
                        (requires (paths-of :X)))
                        
   
   The above example constructs a stricter version of `:X` by insisting 
   all of its paths are required.
"
  [type-signifier-or-map]
  (let [handle-kvs #(->Fork (keys %))]
    (if (map? type-signifier-or-map)
      (handle-kvs (map->flatmap type-signifier-or-map))
      (-> (fn [type-map]
            (handle-kvs ( (includes/includes type-signifier-or-map) type-map)))
          includes/as-type-expander))))

(deftype ALLType []
  CondensedPath 
  (->paths [this] (add-on this [[]]))
  PathComponent
  (add-on [this paths]
    (prn :adding this :onto paths)
    (let [note-index (specter/view (partial map-indexed vector)) ; value [x y] -> [ [0 x] [1 y] ]
                                                                 ; for next step
          prepend-index (specter/collect-one specter/FIRST)  ; stash the index (0 or 1 above) so that
                                                             ; Specter will prepend to final result.
          intermediate-value specter/LAST]  ; Further selectors apply to the original val (x and y)
      (for [path paths]
        (let [x (into path [note-index specter/ALL prepend-index intermediate-value])]
          (prn :result x)
          x)))))

;; TODO: don't know yet if this should reject objects other than Specter-equivalents,
;; or if compilation should.
(extend-type Object
  PathComponent
  (add-on [this paths]
    (map #(conj % this) paths)))

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
            (let [[maps plain] (bifurcate map? preds)
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
