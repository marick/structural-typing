(ns ^:no-doc structural-typing.assist.expred)

(def required-keys #{:predicate :predicate-string :explainer})
(defrecord ExPred [predicate predicate-string explainer])
(def boa ->ExPred) ; Common Lisp joke: a "By Order of Arguments" constructor.

