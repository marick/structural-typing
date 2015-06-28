(ns structural-typing.api.path
  "Functions used in the construction of paths into structures.

   Much of this is gathered into the catchall `structural-typing.types` namespace."
  (:require [com.rpl.specter :as specter]
            [clojure.pprint :refer [cl-format]]
            [structural-typing.frob :as frob]))

(def ^:no-doc friendly-path-components
  {specter/ALL "ALL"})

(def ALL 
  "Use this in a path to select all values of a collection."
  specter/ALL)



(def ^:private type-finder-key ::type-finder)

(defn ^:no-doc type-finder? [x]
  (= type-finder-key (type x)))

(defn includes
  "During creation of a type by `named`, this is replaced with the content the type-key refers to.
   The exact meaning depends on whether it's used in a path, as the value of a path, or
   as an entire argument itself. See the wiki documentation."
  [type-key]
  (when-not (keyword? type-key) (frob/boom "%s is supposed to be a keyword." type-key))
  (-> (fn type-finder [type-map]
        (if-let [result (get type-map type-key)]
          result
          (frob/boom "%s does not name a type" type-key)))
      (with-meta {:type type-finder-key})))

(def required-paths 
  "Used to create an argument to `named`. All of the elements are keys or paths
   that are required (as with [[required-key]]) to be present in any matching
   candidate. This is exactly the same thing as putting the arguments in a vector.

       (type! :Figure (type/required-paths :color 
                                           [:points ALL (type/include :Point)]))

   "
vector)

