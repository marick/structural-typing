(ns ^:no-doc structural-typing.guts.self-check
    (:use structural-typing.clojure.core)
    (:require [structural-typing.guts.expred :as expred]
              [structural-typing.guts.exval :as exval])
    (:refer-clojure :exclude [any?]))

(def types {:expred expred/required-keys
            :exval exval/required-keys
            :oopsie (set-union expred/required-keys exval/required-keys)})

(defn built-like* [type required-keys candidate]
  (let [be-empty (set-difference required-keys (set (keys candidate)))]
    (if (empty? be-empty)
      candidate
      (boom! "Missing keys for %s: %s" type be-empty))))

(defn built-like [type candidate]
  (built-like* type (types type) candidate))

(def returns built-like)
  
(defn returns-many [type candidates]
  (map #(built-like type %1) candidates))
