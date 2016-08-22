(ns structural-typing.use.condensed-type-descriptions.f-strings-as-keys
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil)
  (:refer-clojure :except [any?]))


(start-over!)

(type! :Point (requires "x" "y"))
(type! :Val {"val" integer?})
(type! :Nested {[:k "k"] even?})


(fact "strings are distinct from keys"
  (built-like :Point {"x" 1, "y" 2}) => {"x" 1, "y" 2}
  (check-for-explanations :Point {:x 1, :y 2}) => (just (err:missing ["x"])
                                                        (err:missing ["y"])))

(fact "error messages are built appropriately"
  (built-like :Val {"val" 1}) => {"val" 1}
  (check-for-explanations :Val {"val" "notint"}) => (just (err:shouldbe ["val"] "integer?" "\"notint\"")))

(fact "a nested case"
  (built-like :Nested {:k {"k" 2}}) => {:k {"k" 2}}
  (check-for-explanations :Nested {:k {"k" 3}}) => (just (err:shouldbe [:k "k"] "even?" 3)))

(start-over!)
