(ns structural-typing.paths.readable
    (:require [structural-typing.mechanics.frob :as frob]
              [such.readable :as readable]
              [clojure.pprint :refer [cl-format]])
)

;;; what you type

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


;;; what you see

(defn- stringify [components]
  (if (and (= 1 (count components))
           (keyword? (first components)))
    (str (first components))
    (cl-format nil "[~{~A~^ ~}]" components)))

(defn friendly [path]
  (->> path
       readable/value
       stringify))

