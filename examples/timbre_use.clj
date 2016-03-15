(ns timbre-use
  "An example of logging to Timbre"
  (:require [timbre-define-1 :as v1]
            [timbre-define-2 :as v2]
            [timbre-define-3 :as v3])
  (:use midje.sweet))

(fact "log each explanation as an error"
  (let [result (with-out-str (v1/built-like :Point {:x "1"}))]
    ; (println result)
    result => #"ERROR \[timbre-define-1:\d+\] - :x should be `integer\?`; it is `\"1\"`"
    result => #"ERROR \[timbre-define-1:\d+\] - :y does not exist"))

(fact "a single message; summary first, followed by lines of explanation"
  (let [result (with-out-str (v2/built-like :Point {:x "1"}))]
    ; (println result)
    result => #"ERROR.* - Type failure while checking this:"
    result => #"\{:x \"1\"\}"
    result => #":x should be `integer"
    result => #":y does not exist"))


(fact "each error line + a summary message"
  (let [result (with-out-str (v3/built-like :Point {:x "1"}))]
    ; (println result)
    result => #"INFO.* - While checking this:"
    result => #"INFO.* - \{:x \"1\"\}"
    result => #"INFO \[timbre-define-3:\d+\] - :x should be `integer"
    result => #"INFO \[timbre-define-3:\d+\] - :y does not exist"
    result => #"ERROR \[timbre-define-3:\d+\] - Type check failed"))
