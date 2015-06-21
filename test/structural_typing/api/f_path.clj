(ns structural-typing.api.f-path
  (:require [structural-typing.api.path :as subject]
            [structural-typing.api.predicates :as pred])
  (:use midje.sweet))

(fact "friendly paths"
  (subject/friendly-path {:path [:a]}) => ":a"
  (subject/friendly-path {:path [:a :b]}) => "[:a :b]"
  (subject/friendly-path {:path [:a subject/ALL :b]}) => "[:a ALL :b]"
  (future-fact "handle other specter path components, including plain functions and vars"))
