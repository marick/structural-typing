(ns structural-typing.pred-writing.required-key
  "All of the predefined predicates."
  (:require [structural-typing.pred-writing.lifting :as lifting]
            [structural-typing.pred-writing.shapes.oopsie :as oopsie]
            [structural-typing.pred-writing.shapes.expred :as expred]))

(def required-key
  "False iff a key/path does not exist or has value `nil`. This is the only
   predefined predicate that is not considered optional."
  (lifting/lift-expred (expred/boa (comp not nil?)
                                     "required-key"
                                     #(format "%s must exist and be non-nil"
                                              (oopsie/friendly-path %)))
                         :check-nil))

