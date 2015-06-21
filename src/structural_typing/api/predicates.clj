(ns structural-typing.api.predicates)

(defn must-exist [val]
  (not (nil? val)))
