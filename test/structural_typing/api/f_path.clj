(ns structural-typing.api.f-path
  (:require [structural-typing.api.path :as subject]
            [structural-typing.api.predicates :as pred])
  (:use midje.sweet))

(defn path-n-of [path leaf-index leaf-count]
  {:path path :leaf-index leaf-index :leaf-count leaf-count})
(defn simple-path [path] (path-n-of path 0 1))


(fact "friendly paths"
  (subject/friendly-path (simple-path [:a])) => ":a"
  (subject/friendly-path (simple-path [:a :b])) => "[:a :b]"
  (subject/friendly-path (simple-path [:a subject/ALL :b])) => "[:a ALL :b]")

(fact "cases with indices"
  (subject/friendly-path (path-n-of [:a] 0 5)) => ":a[0]"
  (subject/friendly-path (path-n-of [:a] 1 5)) => ":a[1]"

  (subject/friendly-path (path-n-of [:a :b] 0 5)) => "[:a :b][0]"
  (subject/friendly-path (path-n-of [:a :b] 4 5)) => "[:a :b][4]"

  (subject/friendly-path (path-n-of [:a subject/ALL :b] 0 5)) => "[:a ALL :b][0]"
  (subject/friendly-path (path-n-of [:a subject/ALL :b] 3 20)) => "[:a ALL :b][3]")

(future-fact "handle other specter path components, including plain functions and vars")

