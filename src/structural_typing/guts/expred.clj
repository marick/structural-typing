(ns ^:no-doc structural-typing.guts.expred)


(def required-keys #{:predicate :predicate-string :explainer})
(defrecord ExPred [predicate predicate-string explainer])

