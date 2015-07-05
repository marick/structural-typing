(ns ^:no-doc structural-typing.mechanics.ppps
  "PPP is short for 'path-predicate pair': a vector + a set. Condensed
  ppps have paths that can be used to generate other ppps."
  (:require [structural-typing.frob :as frob]
            [structural-typing.api.path :as path]
            [structural-typing.api.predicates :as pred]
            [clojure.set :as set]))

(def mk vector)
(def path-part first)
(def preds-part second)

(defn mkfn:x-is? [ppp-part]
  (fn [pred] (comp pred ppp-part)))

(def path-is? (mkfn:x-is? path-part))
(def preds-is? (mkfn:x-is? preds-part))

(def forking? (path-is? (partial some sequential?)))
(def required? (preds-is? #(contains? % pred/required-key)))


(defn mkfn:apply-to [ppp-part]
  (fn [f ppp] (f (ppp-part ppp))))
(def apply-to-path (mkfn:apply-to path-part))
(def apply-to-preds (mkfn:apply-to preds-part))
  
(defn path-spreader [f]
  (fn [ppp]
    (map #(mk % (preds-part ppp))
         (apply-to-path f ppp))))


(defn flatten-forked-path
  "Expand a vector containing path elements + shorthand for forks into 
   a vector of paths"
  ([path]
     (flatten-forked-path path [[]]))
       
  ([[x & xs :as path] parent-paths]
     (cond (empty? path)
           parent-paths
           
           (sequential? x)
           (let [extended (for [pp parent-paths, elt x]
                            (conj pp elt))]
             (flatten-forked-path xs (vec extended)))
           
           (map? x)
           (frob/boom "Program error: Path contains a map: %s." path)
           
           :else
           (let [extended (for [pp parent-paths] (conj pp x))]
             (flatten-forked-path xs (frob/force-vector extended))))))




(def unfork-condensed-ppp-paths 
  (frob/mkst:x->abc (path-spreader flatten-forked-path)
                    forking?))

(def add-required-subpaths
  (letfn [(split-path [path]
            (prn :split path)
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
          (->required-ppps [paths]
            (prn :mk-required paths)
            (map mk paths (repeat #{pred/required-key})))]

    (frob/mkst:x->xabc (comp ->required-ppps split-path path-part) required?)))




(defn dc2:ppps->type-description [stream]
  (letfn [(make-map [stream]
            (reduce (fn [so-far [path preds]]
                      (update-in so-far [path] set/union preds))
                    {}
                    stream))
          (vectorize-preds [kvs]
            (frob/update-each-value kvs
                                    #(if (contains? % pred/required-key)
                                       (into [pred/required-key]
                                             (set/difference % #{pred/required-key}))
                                       (vec %))))]
    (-> stream make-map vectorize-preds)))

