(ns structural-typing.assist.special-words
  "There are a number of symbols that have special meanings. They are aggregated here
   for convenience."
  (:use structural-typing.clojure.core)
  (:require structural-typing.guts.preds.core
            structural-typing.guts.preds.annotating
            structural-typing.guts.compile.to-specter-path
            structural-typing.guts.type-descriptions
            structural-typing.guts.type-descriptions.type-expander
            structural-typing.guts.type-descriptions.flatten
            structural-typing.guts.type-descriptions.ppps))

(import-vars [structural-typing.guts.preds.core
               required-path
               required-key
               not-nil]
             [structural-typing.guts.preds.annotating
               show-as
               explain-with]
             [structural-typing.guts.compile.to-specter-path
               ALL
               ONLY
               RANGE]
             [structural-typing.guts.type-descriptions
               requires-mentioned-paths]
             [structural-typing.guts.type-descriptions.type-expander
               includes]
             [structural-typing.guts.type-descriptions.flatten
               through-each each-of paths-of]
             [structural-typing.guts.type-descriptions.ppps
               requires])
             

