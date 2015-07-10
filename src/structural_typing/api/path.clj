(ns structural-typing.api.path
  "Functions used in the construction of paths into structures.

   Much of this is gathered into the catchall `structural-typing.types` namespace."
  (:require [structural-typing.mechanics.m-paths :as path]
            [com.rpl.specter :as specter]
            [clojure.pprint :refer [cl-format]]
            [structural-typing.frob :as frob]))

(def ^:no-doc friendly-path-components
  {specter/ALL "ALL"})

(def ALL 
  "Use this in a path to select all values of a 
   collection.
      
       (type! :Figure {:points ALL (type/include :Point)})
"
  specter/ALL)



(defn includes
  "During creation of a type by [[named]] or [[type!]], a call to
   `includes` is replaced with the type the `type-signifier` refers
   to.  The exact meaning of that replacement depends on whether it's used in a path, as
   the value of a path, or as an entire argument itself. See the wiki
   documentation."
  [type-signifier]
  (when-not (keyword? type-signifier) (frob/boom! "%s is supposed to be a keyword." type-signifier))
  (-> (fn [type-map]
        (if-let [result (get type-map type-signifier)]
          result
          (frob/boom! "%s does not name a type" type-signifier)))
      (with-meta {:type path/type-finder-key})))

(def requires
  "Used to create an argument to [[named]] or [[type!]]. All of the elements are keys or paths
   that are required (as with [[required-key]]) to be present in any matching
   candidate. This is exactly the same thing as putting the arguments in a vector.

       (type! :Figure (type/requires :color 
                                     [:points ALL (type/include :Point)]))

   "
vector)

(def forks
  "When writing a forking path, you may end up with vectors within vectors within
   vectors. You can use `forks` instead of a vector to mark where the path
   forks. 

       (type! :Figure (type/requires [:a (forks :b1 :b2) :c]))

  `forks` is nothing but an alias for `vector`.
"
  vector)


