(ns ^:no-doc structural-typing.guts.type-descriptions
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.type-descriptions.includes :as includes]
            [structural-typing.guts.type-descriptions.ppps :as ppp]
            [structural-typing.guts.compile.compile :as compile]))


(defn ->finished-ppps [condensed-type-descriptions]
  (mapcat ppp/condensed-description->ppps condensed-type-descriptions))

(defn canonicalize [condensed-type-descriptions type-map]
  (when (empty? condensed-type-descriptions)
    (boom! "You must have at least one condensed type description."))
  (-> (includes/substitute type-map condensed-type-descriptions)
      ->finished-ppps
      ppp/->type-description))

(defn lift [condensed-type-descriptions type-map]
  (-> condensed-type-descriptions
      (canonicalize type-map)
      compile/compile-type))
