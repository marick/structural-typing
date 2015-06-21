(ns structural-typing.api.path
  (:require [com.rpl.specter :as specter]
            [structural-typing.frob :as frob]))

(def ^:private friendly-path-components
  {specter/ALL "ALL"})

(defn friendly-path [path]
  (if (= 1 (count path)) (first path) path))



(def ^:private type-finder-key ::type-finder)

(defn type-finder? [x]
  (= type-finder-key (type x)))

(defn a [type-key]
  (when-not (keyword? type-key) (frob/boom "%s is supposed to be a keyword." type-key))
  (-> (fn type-finder [type-map]
        (if-let [result (get type-map type-key)]
          result
          (frob/boom "%s does not name a type" type-key)))
      (with-meta {:type type-finder-key})))
(def an a)

