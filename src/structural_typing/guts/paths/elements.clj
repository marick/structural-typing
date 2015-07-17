(ns ^:no-doc structural-typing.guts.paths.elements
  (:require [structural-typing.guts.frob :as frob]
            [such.readable :as readable])
  (:require [com.rpl.specter :as specter]))

(def ALL 
  "Use this in a path to select all values of a 
   collection.
      
       (type! :Figure {[:points ALL] (type/include :Point)})
"
  specter/ALL)

(defn will-match-many? [elt]
  (boolean (#{ALL} elt)))

;; These define the way special path elements are displayed.
(readable/instead-of ALL 'ALL)

