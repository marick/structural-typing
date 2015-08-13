(ns ^:no-doc structural-typing.pred-writing.shapes.self-check
    (:require [structural-typing.guts.frob :as frob]
              [clojure.set :as set]
              [structural-typing.pred-writing.shapes.expred :as expred]
              [structural-typing.pred-writing.shapes.exval :as exval])
    (:require [such.wide-domains :refer :all]))

(def types {:expred expred/required-keys
            :exval exval/required-keys
            :oopsie (set/union expred/required-keys exval/required-keys)})


(defn checked* [type required-keys candidate]
  (let [be-empty (set/difference required-keys (set (keys candidate)))]
    (if (empty? be-empty)
      candidate
      (frob/boom! "Missing keys for %s: %s" type be-empty))))

(defn checked [type candidate]
  (checked* type (types type) candidate))

(def returns checked)
  
(defn returns-many [type candidates]
  (map #(checked type %1) candidates))
