(ns ^:no-doc structural-typing.guts.shapes.exval)

(def required-keys #{:path :whole-value :leaf-value})
(defrecord ExVal [path whole-value leaf-value])

(def boa ->ExVal)
