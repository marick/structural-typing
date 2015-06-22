(ns structural-typing.mechanics.m-run)

(defn messages [result]
  (map #((:error-explainer %) %) result))

