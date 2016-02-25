(ns ^:no-doc structural-typing.guts.exval)

(def required-keys #{:leaf-value :path :whole-value})
(defrecord ExVal [leaf-value path whole-value])

(defn exval? [x]
  (instance? ExVal x))

(defn exvals? [xs]
  (every? exval? xs))
