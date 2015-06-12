(ns structural-typing.util
  (:refer-clojure :exclude [instance?])
  (:require [clojure.string :as str]
            [bouncer.core :as b])
  (:require [structural-typing.pipeline-stages :as stages]
            [structural-typing.validators :as v]))

(defn update-each-value [kvs f & args]
  (reduce (fn [so-far k] 
            (assoc so-far k (apply f (get kvs k) args)))
          kvs
          (keys kvs)))


(defn mkfn:catcher [f]
  (fn [& xs]
    (try (apply f xs)
    (catch Exception ex false))))

(defn vector-or-wrap [vec-or-not]
  (if (vector? vec-or-not)
    vec-or-not
    (vector vec-or-not)))

