(ns structural-typing.pred-writing.core-preds
  "All of the predefined predicates."
  (:require [structural-typing.guts.shapes.pred :as pred]
            [structural-typing.pred-writing.shapes.oopsie :as oopsie]
            [structural-typing.pred-writing.shapes.expred :as expred]))

(def required-key
  "False iff a key/path does not exist or has value `nil`. 
   
   Note: At some point in the future, this library might make a distinction
   between a `nil` value and a missing key. If so, this predicate will change
   to accept `nil` values. See [[not-nil]].
"
  (pred/lift-expred (expred/boa (comp not nil?)
                                "required-key"
                                #(format "%s must exist and be non-nil"
                                         (oopsie/friendly-path %)))
                    [:check-nil]))

(def not-nil
  "False iff a key/path does not exist or has value `nil`. 
   
   Note: At some point in the future, this library might make a distinction
   between a `nil` value and a missing key. If so, this predicate will change
   to reject `nil` values but be silent about missing keys. See [[required-key]].
"
  (pred/lift-expred (expred/boa (comp not nil?)
                                "not-nil"
                                #(format "%s is nil, and that makes Sir Tony Hoare sad"
                                         (oopsie/friendly-path %)))
                    [:check-nil]))
