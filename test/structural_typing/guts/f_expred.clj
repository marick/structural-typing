(ns structural-typing.guts.f-expred
  (:require [structural-typing.guts.expred :as subject]
            [structural-typing.guts.type-descriptions.elements :refer [ALL]])
  (:use midje.sweet))


(fact "friendly-path"
  (subject/friendly-path {:path [:a]}) => ":a"
  (subject/friendly-path {:path [:a :b]}) => "[:a :b]"
  (subject/friendly-path {:path []}) => "Value")

(fact "default error explainer"
  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x]
                                        :leaf-value 3
                                        })
  => ":x should be `even?`; it is `3`"

  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x ALL :y]
                                        :leaf-value 3})
  => "[:x ALL :y] should be `even?`; it is `3`"


  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x 0 :y]
                                        :leaf-value 3})
  => "[:x 0 :y] should be `even?`; it is `3`"

  (subject/default-predicate-explainer {:predicate-string "even?"
                                        :path [:x odd? :y]
                                        :leaf-value 3})
  => "[:x odd? :y] should be `even?`; it is `3`")


