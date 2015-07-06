(ns ^:no-doc structural-typing.mechanics.m-ppps
  "PPP is short for 'path-predicate pair': a vector + a set. Condensed
  ppps have paths that can be used to generate other ppps."
  (:require [structural-typing.frob :as frob]
            [structural-typing.api.path :as path]
            [structural-typing.mechanics.deriving-paths :as derive]
            [structural-typing.api.predicates :as pred]
            [clojure.set :as set]))

(def ->ppp vector)
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

(defn spread-path-and-x [spreader pred-creator]
  (fn [ppp]
    (map #(->ppp % (pred-creator ppp))
         (apply-to-path spreader ppp))))
  
(defn spread-path-into-required-ppps [spreader]
  (spread-path-and-x spreader (constantly #{pred/required-key})))

(defn spread-path [spreader]
  (spread-path-and-x spreader preds-part))



;;; description decompressors

(def dc:flatmaps->ppps 
  (frob/mkst:x->abc (partial map (fn [[path preds]] (->ppp path (set preds))))))

(def dc:fix-forked-paths 
  (frob/mkst:x->abc (spread-path derive/from-forked-paths)
                    forking?))

(def dc:fix-required-paths-with-collection-selectors
  (frob/mkst:x->xabc (spread-path-into-required-ppps derive/from-paths-with-collection-selectors)
                     required?))


;;; And the final result

(defn ->type-description [stream]
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

