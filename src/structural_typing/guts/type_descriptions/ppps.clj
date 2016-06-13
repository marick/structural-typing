(ns ^:no-doc structural-typing.guts.type-descriptions.ppps
  "PPP is short for 'path-predicate pair': a vector + a set. Condensed
  ppps have paths that can be used to generate other ppps."
  (:use structural-typing.clojure.core)
  (:require [such.readable :as readable]
            [flatland.ordered.set :as os]
            [structural-typing.assist.predicate-defining :as pdef]
            [structural-typing.guts.type-descriptions.flatten :as flatten]
            [structural-typing.guts.preds.pseudopreds :refer [required-path]]))

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

(defn- ordered [original addition]
  (let [destination (if (nil? original)
                      (os/ordered-set)
                      original)
        result (into destination addition)]
    result))

(defn ppps->map [ppps]
  (reduce (fn [so-far ppp]
            (update-in so-far [(:path ppp)] ordered (:preds ppp)))
          {}
          ppps))

(defn vectorize-checkers [kvs]
  (update-each-value kvs
                     #(mapv identity %)))

(defn ->type-description [ppps]
  (-> ppps
      ppps->map
      vectorize-checkers))
