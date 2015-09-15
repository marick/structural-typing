(ns ^:no-doc structural-typing.guts.type-descriptions.ppps
  "PPP is short for 'path-predicate pair': a vector + a set. Condensed
  ppps have paths that can be used to generate other ppps."
  (:use structural-typing.clojure.core)
  (:require [such.readable :as readable]
            [structural-typing.guts.type-descriptions.flatten :as flatten]
            [structural-typing.guts.type-descriptions.elements :as element]
            [structural-typing.guts.preds.core :refer [required-key]]))

(defrecord PPP [path preds])

(defrecord Requires [args])
(defn requires [& args] (->Requires args))

(defmethod clojure.core/print-method Requires [o, ^java.io.Writer w]
  (.write w (readable/value-string (cons 'required (:args o)))))
          

  

(defprotocol DescriptionExpander
  (condensed-description->ppps [this]))

(defn- require-helper [x]
  (->> x
       force-vector
       flatten/->paths
       (map #(->PPP % [required-key]))))

(extend-type Requires
  DescriptionExpander
  (condensed-description->ppps [this]
    (mapcat require-helper (:args this))))

(extend-type clojure.lang.Keyword
  DescriptionExpander
  (condensed-description->ppps [this]
    (require-helper this)))
    
(extend-type clojure.lang.IPersistentMap
  DescriptionExpander
  (condensed-description->ppps [this]
    (map (fn [[k v]] (->PPP k v)) (flatten/map->flatmap this))))

(extend-type clojure.lang.IPersistentVector
  DescriptionExpander
  (condensed-description->ppps [this]
    (boom! "%s is old style description of required keys: use `requires` instead" this)))

;; For an unknown reason, if I extend AFn, the PersistentMap example above fails.
;; So multimethods are checked separately.
(extend-type clojure.lang.Fn
  DescriptionExpander
  (condensed-description->ppps [this]
    (vector (->PPP [] [this]))))

(extend-type clojure.lang.MultiFn
  DescriptionExpander
  (condensed-description->ppps [this]
    (vector (->PPP [] [this]))))



;;; And the final result

(defn- ppps->mapset [ppps]
  (reduce (fn [so-far ppp]
            ;; Note we use set-union instead of concatenating all then converting to set
            ;; because `(into nil [1 2 3])` is not a vector, so this is more convenient
            (update-in so-far [(:path ppp)] set-union (set (:preds ppp))))
          {}
          ppps))

(defn relevant-subvectors [path]
  (->> path
       (map-indexed vector)
       (drop 1) ; This rules out {[ALL :x] [required]}
       (filter #(element/will-match-many? (second %)))
       (map first)
       (map #(subvec path 0 %))))

(defn add-implied-required-keys
  "A form like {[:a ALL :b] [required-key]} implies that the `:a` key must be present.
   That doesn't happen automatically, so a `[:a] [required-key]` term is added."
  [kvs]
  (let [candidate-paths
        (->> kvs
            (filter (fn [[_path_ predset]] (contains? predset required-key)))
            (map first))
        new-paths-with-noise (mapcat relevant-subvectors candidate-paths)
        ;; This prevents sequences like [:x ALL ALL]
        new-paths (remove #(element/will-match-many? (last %)) new-paths-with-noise)]
    (reduce (fn [so-far path]
              (merge-with into so-far (hash-map path #{required-key})))
            kvs
            new-paths)))

(defn- mapset->map-with-ordered-preds [kvs]
  (update-each-value kvs
                     #(if (contains? % required-key)
                        (into [required-key]
                              (set-difference % #{required-key}))
                        (vec %))))

(defn ->type-description [ppps]
  (-> ppps ppps->mapset add-implied-required-keys mapset->map-with-ordered-preds))

