(ns structural-typing.guts.type-descriptions.whole-value-preds
  (:use structural-typing.clojure.core))

(def dc:preds->maps
  (lazyseq:x->y #(hash-map [] [%]) extended-fn?))

