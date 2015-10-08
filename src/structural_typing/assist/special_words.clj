(ns structural-typing.assist.special-words
  "There are a number of symbols that have special meanings. They are aggregated here
   for convenience."
  (:use structural-typing.clojure.core))


(import-vars [structural-typing.guts.preds.core
               required-path
               required-key
               not-nil]
             [structural-typing.guts.preds.annotating
               show-as
               explain-with]
             [structural-typing.guts.compile.to-specter-path
               ALL
               RANGE]
             [structural-typing.guts.type-descriptions
               requires-mentioned-paths]
             [structural-typing.guts.type-descriptions.includes
               includes]
             [structural-typing.guts.type-descriptions.flatten
               through-each each-of paths-of]
             [structural-typing.guts.type-descriptions.ppps
               requires])
             

