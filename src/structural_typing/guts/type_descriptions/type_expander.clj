(ns ^:no-doc structural-typing.guts.type-descriptions.type-expander
  "A type expander is a function that takes a type map and performs
   type substitution in a (lexically bound) condensed type description(s)."
  (:use structural-typing.clojure.core)
  (:require [com.rpl.specter :as specter]
            [such.metadata :as meta]))

;;;;                             The (includes :Point) mechanism

(def type-expander-key ::type-expander)

(defn- type-expander? [x]
  (boolean (meta/get x type-expander-key)))

(defn as-type-expander [x]
  (meta/assoc x type-expander-key true))

(defmacro mkfn [[type-map-sym] & body]
  `(-> (fn [~type-map-sym] ~@body)
       as-type-expander))

(defn expand-throughout [type-map forms]
  (specter/transform (specter/walker type-expander?)
                     #(% type-map)
                     forms))

(defn includes
  "During creation of a type by [[named]] or [[type!]], a call to
   `includes` is replaced with the type the `type-signifier` refers to."
  [type-signifier]
  (when-not (keyword? type-signifier) (boom! "%s is supposed to be a keyword." type-signifier))
  (mkfn [type-map] 
     (if-let [result (get type-map type-signifier)]
       result
       (boom! "%s does not name a type" type-signifier))))

