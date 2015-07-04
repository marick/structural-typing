(ns structural-typing.api.f-defaults
  (:require [structural-typing.api.defaults :as subject]
            [structural-typing.api.path :as path])
  (:use midje.sweet))

(fact "default error explainer"
  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x]
                                        :leaf-value 3
                                        :leaf-index 0
                                        :leaf-count 1
                                        })
  => ":x should be `even?`; it is `3`"

  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x path/ALL :y]
                                        :leaf-value 3
                                        :leaf-index 0
                                        :leaf-count 2})
  => "[:x ALL :y][0] should be `even?`; it is `3`")


