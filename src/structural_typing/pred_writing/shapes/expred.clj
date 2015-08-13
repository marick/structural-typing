(ns ^:no-doc structural-typing.pred-writing.shapes.expred)

(def required-keys #{:predicate :predicate-string :explainer})
(defrecord ExPred [predicate predicate-string explainer])
(def boa ->ExPred)

