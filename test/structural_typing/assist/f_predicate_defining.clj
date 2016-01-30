(ns structural-typing.assist.f-predicate-defining
  (:require [structural-typing.assist.predicate-defining :as subject]
            [midje.sweet :refer :all]))

(defrecord R [a b])

(fact "record classes can be printed more nicely"
  (subject/pretty-record-class (->R 1 2)) => "R")

(fact "records are prettier if printed without the whole namespace"
  (subject/pretty-record-instance (->R 1 2)) => "#R{:a 1, :b 2}")
