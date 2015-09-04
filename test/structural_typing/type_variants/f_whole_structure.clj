(ns structural-typing.type-variants.f-whole-structure
  (:use structural-typing.type
        structural-typing.global-type)
  (:use midje.sweet structural-typing.assist.testutil))

(start-over!)

(tabular
  (fact
    (type! ?signifier ?body)
    (checked ?signifier "string") => "string"
    (check-for-explanations ?signifier 1) => (just #"Value should be `string\?`; it is `1`"))
  ?signifier   ?body
  :Sugared     string?
  :Unsugared   {[] string?})

(start-over!)
