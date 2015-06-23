(ns structural-typing.api.path
  (:require [com.rpl.specter :as specter]
            [clojure.pprint :refer [cl-format]]
            [structural-typing.type-repo :as type-repo]
            [structural-typing.frob :as frob]))

(def friendly-path-components
  {specter/ALL "ALL"})

(def ALL specter/ALL)



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

