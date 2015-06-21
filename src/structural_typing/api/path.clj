(ns structural-typing.api.path)

(defn friendly-path [path]
  (if (= 1 (count path)) (first path) path))
