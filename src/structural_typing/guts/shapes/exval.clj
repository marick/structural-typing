(ns ^:no-doc structural-typing.guts.shapes.exval)

(defrecord ExVal [path whole-value leaf-value])

(defn exvals-for-leafs [path whole-value leafs]
  (map #(hash-map :path path :whole-value whole-value :leaf-value %1) leafs))
