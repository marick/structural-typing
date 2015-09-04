(ns ^:no-doc structural-typing.guts.paths.multiplying
    (:require [structural-typing.guts.frob :as frob]
              [structural-typing.assist.core-preds :refer [required-key]]))

(def forking? (partial some sequential?))

(defn forked-paths
  "Expand a vector containing path elements + shorthand for forks into 
   a vector of paths"
  ([path]
     (forked-paths path [[]]))
       
  ([[x & xs :as path] parent-paths]
     (cond (empty? path)
           parent-paths
           
           (sequential? x)
           (let [extended (for [pp parent-paths, elt x]
                            (conj pp elt))]
             (forked-paths xs (vec extended)))
           
           (map? x)
           (frob/boom! "Program error: Path contains a map: %s." path)
           
           :else
           (let [extended (for [pp parent-paths] (conj pp x))]
             (forked-paths xs (frob/force-vector extended))))))


(def required? #(contains? % required-key))

(defn required-prefix-paths [path]
  (reduce (fn [so-far [prefix current]]
            (cond (keyword? current)
                  so-far
                  
                  ;; This is the [... ALL ALL ...] case
                  (not (keyword? (last prefix)))
                  so-far
                  
                  :else 
                  (conj so-far prefix)))
          []
          (map vector (reductions conj [] path) path)))


