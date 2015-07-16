(ns ^:no-doc structural-typing.mechanics.canonicalizing-types
  (:require [such.function-makers :as mkfn])
  (:require [structural-typing.frob :as frob]
            [structural-typing.mechanics.m-ppps :as ppp]
            [structural-typing.mechanics.m-paths :as path]
            [structural-typing.mechanics.m-maps :as map]
            [structural-typing.api.predicates :as pred]
            [com.rpl.specter :as specter]))

;;; Decompressers undo one or more types of compression allowed in compressed type descriptions.

(defn dc:expand-type-signifiers [type-map form]
  (let [do-one #(if (path/type-finder? %) (% type-map) %)]
    (specter/transform (specter/walker path/type-finder?) do-one form)))

(def dc:validate-starting-descriptions
  (mkfn/lazyseq:criticize-deviationism
   (mkfn/pred:none-of? map? sequential?)
   #(frob/boom! "Types are described with maps or vectors: `%s` has `%s`"
           %1 %2)))

(def dc:spread-collections-of-required-paths
  (mkfn/lazyseq:x->abc (partial map frob/force-vector) (complement map?)))

(def dc:split-paths-ending-in-maps

  (mkfn/lazyseq:x->abc #(let [prefix-path (pop %)]
                       (vector prefix-path (hash-map prefix-path (last %))))
                    path/ends-in-map?))
                    
(def dc:required-paths->maps 
  (mkfn/lazyseq:x->y #(hash-map % [pred/required-key]) (complement map?)))

(def dc:flatten-maps
  (mkfn/lazyseq:x->y map/flatten-map map?))


(defn canonicalize [type-map & condensed-type-descriptions]
  (when (empty? condensed-type-descriptions)
    (frob/boom! "Canonicalize was called with no type descriptions. Type-map: %s" type-map))

  (->> condensed-type-descriptions
       (dc:expand-type-signifiers type-map) ; comes first because signifiers can be top-level
       dc:validate-starting-descriptions

       ;; Let's work with the vectors of required paths, ending up with maps
       dc:spread-collections-of-required-paths      
       dc:split-paths-ending-in-maps   ; can preoduce a new map
       dc:required-paths->maps         ; path may still contain forks

       dc:flatten-maps

       ppp/dc:flatmaps->ppps
       ppp/dc:fix-forked-paths
       ppp/dc:fix-required-paths-with-collection-selectors

       ppp/->type-description))
