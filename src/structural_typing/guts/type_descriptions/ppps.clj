(ns ^:no-doc structural-typing.guts.type-descriptions.ppps
  "PPP is short for 'path-predicate pair': a vector + a set. Condensed
  ppps have paths that can be used to generate other ppps."
  (:use structural-typing.clojure.core)
  (:require [such.readable :as readable]
            [structural-typing.assist.predicate-defining :as pdef]
            [structural-typing.guts.type-descriptions.flatten :as flatten]
            [structural-typing.guts.compile.to-specter-path :as to-specter-path]
            [structural-typing.guts.preds.core :refer [required-path]]))

(defrecord PPP [path preds])

(defrecord Requires [args])
(defn requires
  "Often, all you want to say about some parts of a type is that they're required.
   `requires` is a shorthand way to do that.
   
       (type! :Point (requires :x :y))
       (type! :Line (requires [:start :x] [:start :y]))
       (type! :Line (requires [(through-each :start :end) (:each-of :x :y)]))
"
  [& args] (->Requires args))

(defmethod clojure.core/print-method Requires [o, ^java.io.Writer w]
  (.write w (readable/value-string (cons 'required (:args o)))))
          

  

(defprotocol DescriptionExpander
  (condensed-description->ppps [this]))

(defn- require-helper [x]
  (->> x
       force-vector
       flatten/->paths
       (map #(->PPP % [required-path]))))

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
    (boom! "%s is the old-style description of required keys: use `requires` instead" this)))

;; For an unknown reason, if I extend AFn, the PersistentMap example above fails.
;; So multimethods are built-like separately.
(extend-type clojure.lang.Fn
  DescriptionExpander
  (condensed-description->ppps [this]
    (vector (->PPP [] [this]))))

(extend-type clojure.lang.MultiFn
  DescriptionExpander
  (condensed-description->ppps [this]
    (vector (->PPP [] [this]))))

(extend-type java.lang.Object
  DescriptionExpander
  (condensed-description->ppps [this]
    (boom! "Error in a condensed type description: `%s` is not allowed" this)))

(extend-type nil
  DescriptionExpander
  (condensed-description->ppps [this]
    (boom! "One of your condensed type descriptions evaluated to `nil`")))

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
       (filter #(to-specter-path/will-match-many? (second %)))
       (map first)
       (map #(subvec path 0 %))))

(defn add-implied-required-paths
  "A form like {[:a ALL :b] [required-path]} implies that the `:a` key must be present.
   That doesn't happen automatically, so a `[:a] [required-path]` term is added."
  [kvs]
  (let [candidate-paths
        (->> kvs
            (filter (fn [[_path_ predset]] (contains? predset required-path)))
            (map first))
        new-paths-with-noise (mapcat relevant-subvectors candidate-paths)
        ;; This prevents sequences like [:x ALL ALL]
        new-paths (remove #(to-specter-path/will-match-many? (last %)) new-paths-with-noise)]
    (reduce (fn [so-far path]
              (merge-with into so-far (hash-map path #{required-path})))
            kvs
            new-paths)))

(defn force-predicate [value]
  (let [converter (branch-on value
                             extended-fn?   identity
                             regex?         pdef/regex-match
                             number?        pdef/number-match
                             record?        pdef/record-match
                             :else          pdef/exactly)]
    (converter value)))

(defn coerce-plain-values-into-predicates [kvs]
  (update-each-value kvs
                     #(set (map force-predicate %))))

(defn- mapset->map-with-ordered-preds [kvs]
  (update-each-value kvs
                     #(if (contains? % required-path)
                        (into [required-path]
                              (set-difference % #{required-path}))
                        (vec %))))

(defn ->type-description [ppps]
  (-> ppps
      ppps->mapset
      add-implied-required-paths
      coerce-plain-values-into-predicates
      mapset->map-with-ordered-preds))
