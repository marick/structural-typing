(ns ^:no-doc structural-typing.guts.mechanics.canonicalizing-types
  (:require [such.function-makers :as mkfn])
  (:require [structural-typing.guts.frob :as frob]
            [structural-typing.guts.mechanics.m-ppps :as ppp]
            [structural-typing.guts.mechanics.m-maps :as map]
            [structural-typing.pred-writing.core-preds :refer [required-key]]
            [com.rpl.specter :as specter]))

;;; Decompressers undo one or more types of compression allowed in compressed type descriptions.
;;; Decompressors written elsewhere are imported here so that tests can easily show how they
;;; play together.

(frob/import-vars [structural-typing.guts.paths.substituting
                      dc:expand-type-signifiers dc:split-paths-ending-in-maps])

(def dc:validate-starting-descriptions
  (mkfn/lazyseq:criticize-deviationism
   (mkfn/pred:none-of? frob/extended-fn? map? sequential? keyword?)
   #(frob/boom! "Types are described with maps, functions, vectors, or keywords: `%s` has `%s`"
                %1 %2)))

(def dc:preds->maps
  (mkfn/lazyseq:x->y #(hash-map [] [%]) frob/extended-fn?))

(def dc:spread-collections-of-required-paths
  (mkfn/lazyseq:x->abc (partial map frob/force-vector) (complement map?)))

(def dc:keywords-to-required-maps
  (mkfn/lazyseq:x->y vector keyword?))

(def dc:required-paths->maps 
  (mkfn/lazyseq:x->y #(hash-map % [required-key]) (complement map?)))

(def dc:flatten-maps
  (mkfn/lazyseq:x->y map/flatten-map map?))


(defn canonicalize [type-map & condensed-type-descriptions]
  (when (empty? condensed-type-descriptions)
    (frob/boom! "Canonicalize was called with no type descriptions. Type-map: %s" type-map))

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

       dc:flatten-maps

       ppp/dc:flatmaps->ppps
       ppp/dc:fix-forked-paths
       ppp/dc:fix-required-paths-with-collection-selectors

       ppp/->type-description))
