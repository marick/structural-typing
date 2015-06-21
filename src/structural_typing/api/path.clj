(ns structural-typing.api.path
  (:require [com.rpl.specter :as specter]
            [clojure.pprint :refer [cl-format]]
            [structural-typing.frob :as frob]))

(def ^:private friendly-path-components
  {specter/ALL "ALL"})

(def ALL specter/ALL)

(defn friendly-path-component [component]
  (cond (contains? friendly-path-components component)
        (friendly-path-components component)

        :else
        (str component)))

(defn friendly-path [{:keys [path] :as explanation}]
  (let [tokens (map friendly-path-component path)]
    (if (= 1 (count tokens))
      (first tokens)
      (cl-format nil "[~{~A~^ ~}]" tokens))))


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

