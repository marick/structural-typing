(ns ^:no-doc structural-typing.clojure.core
  "The general purpose functions that, for purposes of this application, I wish
   had been included in clojure.core."
  (:require [such.immigration :as immigrate]))

(immigrate/import-vars [such.maps 
                         mkmap:all-keys-with-value
                         update-each-value]
                       [such.wrongness
                         boom!]
                       [such.types
                         extended-fn?]
                       [such.immigration
                        import-vars]
                       [clojure.pprint
                        cl-format pprint]
                       [swiss.arrows
                        -<> -!> -!>> -<>> some-<> some-<>>])
                       
(immigrate/import-all-vars such.shorthand
                           such.function-makers
                           such.types)
(immigrate/import-prefixed-vars clojure.set set-)
(immigrate/import-prefixed-vars clojure.string str-)

(defn force-vector [v]
  (cond (vector? v) v
        (sequential? v) (vec v)
        :else (vector v)))

(defn adding-on [coll maybe-vector]
  (into coll (force-vector maybe-vector)))


(defn alternately [evenf oddf & colls]
  (apply map
         (fn [f & args] (apply f args))
         (cycle [evenf oddf])
         colls))

