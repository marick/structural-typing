(ns ^:no-doc structural-typing.guts.type-descriptions.elements
  (:use structural-typing.clojure.core)
  (:require [such.readable :as readable]
            [such.metadata :as meta]
            [com.rpl.specter :as specter]))

;;; TODO: This is a rather kludgy way of handling things.

(defn mkfn:meta-getter [key]
  (fn [elt] (meta/get elt key)))

(def specter-equivalent (mkfn:meta-getter :specter-equivalent))
(def will-match-many? (comp boolean (mkfn:meta-getter :will-match-many?)))


(def ALL
  "Use this in a path to select all values of a 
   collection.
      
       (type! :Figure {[:points ALL] (type/include :Point)})
"
  (meta/assoc 'ALL :specter-equivalent [specter/ALL] :will-match-many? true))

;; TODO: In suchwow 3, have to use `instead-of`, which is lame, because it has global
;; state. such.readable needs fixing.
(defn RANGE
  "Use this in a path to select a range of values in a 
   collection. The first argument is inclusive; the second exclusive.
   
       (type! :ELEMENTS-1-AND-2-ARE-EVEN {[(RANGE 1 3)] even?})
"
  [inclusive-start exclusive-end]
  (let [r (meta/assoc (gensym (format "RANGE-%s-%s" inclusive-start exclusive-end))
                      :specter-equivalent [(specter/srange inclusive-start exclusive-end) specter/ALL]
                      :will-match-many? true)]
    (readable/instead-of r (list 'RANGE inclusive-start exclusive-end))
    r))
