(ns ^:no-doc structural-typing.mechanics.deriving-paths
  (:require [structural-typing.frob :as frob]))

(defn from-forked-paths
  "Expand a vector containing path elements + shorthand for forks into 
   a vector of paths"
  ([path]
     (from-forked-paths path [[]]))
       
  ([[x & xs :as path] parent-paths]
     (cond (empty? path)
           parent-paths
           
           (sequential? x)
           (let [extended (for [pp parent-paths, elt x]
                            (conj pp elt))]
             (from-forked-paths xs (vec extended)))
           
           (map? x)
           (frob/boom! "Program error: Path contains a map: %s." path)
           
           :else
           (let [extended (for [pp parent-paths] (conj pp x))]
             (from-forked-paths xs (frob/force-vector extended))))))

(defn from-paths-with-collection-selectors [path]
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


