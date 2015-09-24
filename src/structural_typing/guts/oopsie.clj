(ns ^:no-doc structural-typing.guts.oopsie
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.expred :as expred]))

(import-vars [structural-typing.guts.expred friendly-path])

(defn parts->oopsie [& parts]
  (apply merge parts))
  
