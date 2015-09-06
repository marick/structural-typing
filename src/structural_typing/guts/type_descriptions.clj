(ns structural-typing.guts.type-descriptions
  (:require [structural-typing.guts.type-descriptions.canonicalizing :as canon]
            [structural-typing.guts.preds.from-type-descriptions :as compile]))

(defn canonicalize [condensed-type-descriptions type-map]
  (apply canon/canonicalize type-map condensed-type-descriptions))

(defn lift [condensed-type-descriptions type-map]
  (-> condensed-type-descriptions
      (canonicalize type-map)
      compile/compile-type))
