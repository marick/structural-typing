(ns timbre-use
  "An example of logging to Timbre"
  (:require [timbre-define-1 :as v1]
            [timbre-define-2 :as v2])
  (:use midje.sweet))

(fact "log each explanation as an error"
  (let [result (with-out-str (v1/built-like :Point {:x "1"}))]
    ; (println result)
    result => #"ERROR \[timbre-define-1:\d+\] - :x should be `integer\?`; it is `\"1\"`"
    result => #"ERROR \[timbre-define-1:\d+\] - :y does not exist"))

(fact "more verbose error handling"
  (let [result (with-out-str (v2/built-like :Point {:x "1"}))]
    ; (println result)
    result => #"INFO.* - While checking this:"
    result => #"INFO.* - \{:x \"1\"\}"
    result => #"INFO \[timbre-define-2:\d+\] - :x should be `integer"
    result => #"INFO \[timbre-define-2:\d+\] - :y does not exist"
    result => #"ERROR \[timbre-define-2:\d+\] - Boundary type check failed"))
