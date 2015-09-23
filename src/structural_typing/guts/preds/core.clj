(ns structural-typing.guts.preds.core
  "Preds that are used througout"
  (:require [structural-typing.guts.preds.wrap :as wrap]
            [structural-typing.guts.expred :as expred]))

(def required-key
  "False iff a key/path does not exist or has value `nil`. 
   
   Note: At some point in the future, this library might make a distinction
   between a `nil` value and a missing key. If so, this predicate will change
   to accept `nil` values. See [[not-nil]].
"
  (wrap/lift-expred (expred/->ExPred (comp not nil?)
                                     "required-key"
                                     #(format "%s must exist and be non-nil"
                                              (expred/friendly-path %)))
                    [:check-nil]))

(def not-nil
  "False iff a key/path does not exist or has value `nil`. 
   
   Note: At some point in the future, this library might make a distinction
   between a `nil` value and a missing key. If so, this predicate will change
   to reject `nil` values but be silent about missing keys. See [[required-key]].
"
  (wrap/lift-expred (expred/->ExPred (comp not nil?)
                                     "not-nil"
                                     #(format "%s is nil, and that makes Sir Tony Hoare sad"
                                              (expred/friendly-path %)))
                    [:check-nil]))
