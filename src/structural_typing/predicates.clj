(ns structural-typing.predicates)

(defn must-exist [val]
  (not (nil? val)))
