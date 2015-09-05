(ns ^:no-doc structural-typing.guts.type-descriptions.canonicalizing
  (:use structural-typing.clojure.core)
  (:require [such.sequences :as seq]
            [com.rpl.specter :as specter])
  (:require [structural-typing.guts.type-descriptions.m-ppps :as ppp]
            [structural-typing.guts.type-descriptions.m-maps :as map]
            [structural-typing.assist.core-preds :refer [required-key]]))

;;; Decompressers undo one or more types of compression allowed in compressed type descriptions.
;;; Decompressors written elsewhere are imported here so that tests can easily show how they
;;; play together.

(import-vars [structural-typing.guts.type-descriptions.substituting
              dc:expand-type-signifiers dc:split-paths-ending-in-maps])

(def dc:validate-starting-descriptions
  (lazyseq:criticize-deviationism
   (pred:none-of? extended-fn? map? sequential? keyword?)
   #(boom! "Types are described with maps, functions, vectors, or keywords: `%s` has `%s`"
           %1 %2)))

(def dc:preds->maps
  (lazyseq:x->y #(hash-map [] [%]) extended-fn?))

(def dc:spread-collections-of-required-paths
  (lazyseq:x->abc (partial map force-vector) (complement map?)))

(def dc:keywords-to-required-maps
  (lazyseq:x->y vector keyword?))

(def dc:required-paths->maps 
  (lazyseq:x->y #(hash-map % [required-key]) (complement map?)))

(def dc:allow-includes-in-preds
  (lazyseq:x->abc
   (fn [kvs]
     (loop [plain-pred-map {}
            type-valued-maps []
            [ [path preds] & kvs] (map identity kvs)]
       (cond (nil? path)
             (conj type-valued-maps plain-pred-map)

             (not (sequential? preds))
             (recur (assoc plain-pred-map path preds)
                    type-valued-maps
                    kvs)
       
             :else
             (let [[types plain] (seq/bifurcate map? preds)]
               (recur (if (empty? plain) plain-pred-map (assoc plain-pred-map path plain))
                      (into type-valued-maps (map #(hash-map path %) types))
                      kvs)))))))

(def dc:flatten-maps
  (lazyseq:x->y map/flatten-map map?))


(defn canonicalize [type-map & condensed-type-descriptions]
  (when (empty? condensed-type-descriptions)
    (boom! "Canonicalize was called with no type descriptions. Type-map: %s" type-map))

  (->> condensed-type-descriptions
       (dc:expand-type-signifiers type-map) ; comes first because signifiers can be top-level
       dc:validate-starting-descriptions

       ;; predicates
       dc:preds->maps

       ;; Let's work with the vectors of required paths, ending up with maps
       dc:keywords-to-required-maps
       dc:spread-collections-of-required-paths      
       dc:split-paths-ending-in-maps   ; can produce a new map
       dc:required-paths->maps         ; path may still contain forks

       dc:allow-includes-in-preds
       dc:flatten-maps

       ppp/dc:flatmaps->ppps
       ppp/dc:fix-forked-paths
       ppp/dc:fix-required-paths-with-collection-selectors

       ppp/->type-description))
