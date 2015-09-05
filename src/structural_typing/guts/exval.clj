(ns ^:no-doc structural-typing.guts.exval)

(def required-keys #{:leaf-value :path :whole-value})
(defrecord ExVal [leaf-value path whole-value])

(def boa ->ExVal) ; Common Lisp joke: a "By Order of Arguments" constructor.
