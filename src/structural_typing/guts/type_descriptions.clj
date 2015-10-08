(ns ^:no-doc structural-typing.guts.type-descriptions
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.type-descriptions.includes :as includes]
            [structural-typing.guts.type-descriptions.ppps :as ppp]
            [structural-typing.guts.compile.compile :as compile]
            [structural-typing.guts.preds.core :refer [required-key]]))


(defn ->finished-ppps [condensed-type-descriptions]
  (mapcat ppp/condensed-description->ppps condensed-type-descriptions))

(defn canonicalize [condensed-type-descriptions type-map]
  (-> (includes/substitute type-map condensed-type-descriptions)
      ->finished-ppps
      ppp/->type-description))

(defn lift [condensed-type-descriptions type-map]
  (-> condensed-type-descriptions
      (canonicalize type-map)
      compile/compile-type))

(defn requires-mentioned-paths
  "Forces each path in the (canonicalized) type descriptions to be required as if
   you'd used [[required-key]] with it.
   
        (type! :X (requires-mentioned-paths (includes :Point)
                                            (includes :Colorful))
  
   Note: It can't require paths you don't mention. The easiest way to mention a
   path is to name it in a `requires` - which may be either an argument to this function
   or outside it:
   
        (type! :X (requires-mentioned-paths (requires :name)
                                            (includes :Point)))
        (type! :X (requires :name)
                  (requires-mentioned-paths (includes :Point)))
"
  [& condensed-type-descriptions]
  (-> (fn [type-map]
        (let [canonical (canonicalize condensed-type-descriptions type-map)]
          (update-each-value canonical conj required-key)))
      includes/as-type-expander))

