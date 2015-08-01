(ns ^:no-doc structural-typing.guts.paths.elements
  (:require [structural-typing.guts.frob :as frob]
            [such.readable :as readable]
            [such.metadata :as meta])
  (:require [com.rpl.specter :as specter]))

(defn mkfn:meta-getter [key]
  (fn [elt] (meta/get elt key)))

(def offset (mkfn:meta-getter :offset))
(def specter-equivalent (mkfn:meta-getter :specter-equivalent))
(def will-match-many? (comp boolean (mkfn:meta-getter :will-match-many?)))

(def ALL
  "Use this in a path to select all values of a 
   collection.
      
       (type! :Figure {[:points ALL] (type/include :Point)})
"
  (meta/assoc 'ALL :specter-equivalent [specter/ALL] :offset 0 :will-match-many? true))

;; TODO: In suchwow 3, have to use `instead-of`, which is lame, because it has global
;; state. such.readable needs fixing.
(defn RANGE [inclusive-start exclusive-end]
  (let [r (meta/assoc 'RANGE
                      :specter-equivalent [(specter/srange inclusive-start exclusive-end) specter/ALL]
                      :offset inclusive-start
                      :will-match-many? true)]
    (readable/instead-of r (list 'RANGE inclusive-start exclusive-end))
    r))
