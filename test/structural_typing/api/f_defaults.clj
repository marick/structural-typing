(ns structural-typing.api.f-defaults
  (:require [structural-typing.api.defaults :as subject]
            [structural-typing.api.path :as path])
  (:use midje.sweet))

(fact "default error explainer"
  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x]
                                        :leaf-value 3
                                        })
  => ":x should be `even?`; it is `3`"

  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x path/ALL :y]
                                        :leaf-value 3})
  =future=> "[:x 0 :y] should be `even?`; it is `3`")


