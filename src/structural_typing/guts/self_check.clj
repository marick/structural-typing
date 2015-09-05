(ns ^:no-doc structural-typing.guts.self-check
    (:use structural-typing.clojure.core)
    (:require [structural-typing.assist.expred :as expred]
              [structural-typing.assist.exval :as exval]))

(def types {:expred expred/required-keys
            :exval exval/required-keys
            :oopsie (set-union expred/required-keys exval/required-keys)})

(defn checked* [type required-keys candidate]
  (let [be-empty (set-difference required-keys (set (keys candidate)))]
    (if (empty? be-empty)
      candidate
      (boom! "Missing keys for %s: %s" type be-empty))))

(defn checked [type candidate]
  (checked* type (types type) candidate))

(def returns checked)
  
(defn returns-many [type candidates]
  (map #(checked type %1) candidates))
