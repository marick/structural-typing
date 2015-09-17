(ns structural-typing.assist.special-words
  "There are a number of symbols that have special meanings. They are aggregated here
   for convenience."
  (:use structural-typing.clojure.core))


(import-vars [structural-typing.guts.preds.core
               required-key
               not-nil]
             [structural-typing.guts.preds.annotating
               show-as
               explain-with]
             [structural-typing.guts.type-descriptions.elements
               ALL
               RANGE]
             [structural-typing.guts.type-descriptions.includes
               includes]
             [structural-typing.guts.type-descriptions.flatten
               through-each each-of paths-of]
             [structural-typing.guts.type-descriptions.ppps
               requires])
             

