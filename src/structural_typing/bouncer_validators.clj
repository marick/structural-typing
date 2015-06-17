(ns structural-typing.bouncer-validators
  "A facade over bouncer, decoupling (somewhat) this library from its details"
  (:require [structural-typing.frob :as frob]
            [bouncer.core :as b]))


(defn check [type-repo type-signifier candidate]
  (b/validate (:error-explanation-producer type-repo)
              candidate
              (get-in type-repo [:validators type-signifier])))

(defn flatten-path-representation
  "Convert an atom into a vector of that sequential.
   Convert a sequential into a flattened vector.
   A vector with a subvector of length N produces N flattened vectors."
  ([v]
     (if (sequential? v)
       (flatten-path-representation [[]] v)
       (vector v)))
       
  ([parent-paths v]
     (cond (empty? v)
           parent-paths

           (sequential? (first v))
           (let [extended (for [pp parent-paths, elt (first v)]
                            (conj pp elt))]
             (flatten-path-representation (vec extended) (rest v)))

           :else
           (let [extended (for [pp parent-paths] (conj pp (first v)))]
             (flatten-path-representation (vec extended) (rest v))))))


(defn flatten-N-path-representations [v]
  (vec (mapcat flatten-path-representation v)))


