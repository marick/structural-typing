(ns structural-typing.assist.special-words
  (:use structural-typing.clojure.core))


(import-vars [structural-typing.guts.preds.core
               required-key
               not-nil]
             [structural-typing.guts.type-descriptions.elements
               ALL
               RANGE]
             [structural-typing.guts.type-descriptions.dc-type-maps
               includes]
             [structural-typing.guts.type-descriptions.flatten
               through-each each-of paths-of]
             [structural-typing.guts.type-descriptions.m-ppps
               requires])
             

