(ns structural-typing.paths.elements
  (:require [structural-typing.mechanics.frob :as frob])
  (:require [com.rpl.specter :as specter]))

(def ALL 
  "Use this in a path to select all values of a 
   collection.
      
       (type! :Figure {[:points ALL] (type/include :Point)})
"
  specter/ALL)

(defn will-match-many? [elt]
  (boolean (#{ALL} elt)))

