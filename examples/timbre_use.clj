(ns timbre-use
  "An example of logging to Timbre"
  (:require [timbre-define-1 :as v1]
            [timbre-define-2 :as v2])
  (:use midje.sweet))

(fact "log each explanation as an error"
  (let [result (with-out-str (v1/built-like :Point {:x "1"}))]
    result => #"ERROR \[timbre-define-1\]"
    result => #":x should be `integer"
    result => #":y must exist and be non-nil"))

(fact "more verbose error handling"
  (let [result (with-out-str (v2/built-like :Point {:x "1"}))]
    result => #"INFO \[timbre-define-2\] - :x should be `integer"
    result => #"INFO \[timbre-define-2\] - :y must exist"
    result => #"ERROR \[timbre-define-2\] - Boundary type check"))
