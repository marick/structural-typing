(ns ^:no-doc structural-typing.guts.paths.elements
  (:require [structural-typing.guts.frob :as frob]
            [such.readable :as readable]
            [such.metadata :as meta])
  (:require [com.rpl.specter :as specter]))

(def ALL
  "Use this in a path to select all values of a 
   collection.
      
       (type! :Figure {[:points ALL] (type/include :Point)})
"
  (meta/assoc 'ALL :specter-equivalent [specter/ALL] :offset 0 :will-match-many? true))

(defn mkfn:meta-getter [key]
  (fn [elt] (meta/get elt key)))

(def offset (mkfn:meta-getter :offset))
(def specter-equivalent (mkfn:meta-getter :specter-equivalent))
(def will-match-many? (comp boolean (mkfn:meta-getter :will-match-many?)))

