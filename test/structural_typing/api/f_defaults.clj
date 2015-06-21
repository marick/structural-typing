(ns structural-typing.api.f-defaults
  (:require [structural-typing.api.defaults :as subject])
  (:use midje.sweet))

(future-fact "default error explainer"
  (subject/default-error-explainer {:predicate-string "core/even?"
                                    :path [:x]
                                    :leaf-value 3})
  => ":x should be `core/even?`; it is `3`"


  (subject/default-error-explainer {:predicate-string "core/even?"
                                    :path [:x ALL :y]
                                    :leaf-value 3
                                    :index-string "[0]"})
  => "[:x ALL :y] should be `core/even?`; it is `3`"

)

