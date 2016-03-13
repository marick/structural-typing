(ns use
  "Ways of using a type repo defined in a namespace"
  (:require [definition :as mytypes])
  (:use midje.sweet))

(fact "The standard functions work with valid points"
  (let [ok {:x 1 :y 1}]
    (mytypes/built-like? :Point ok) => true

    (mytypes/built-like :Point ok) => ok
    (mytypes/<>built-like ok :Point) => ok

    (mytypes/all-built-like :Point [ok ok]) => [ok ok]
    (mytypes/<>all-built-like [ok ok] :Point) => [ok ok]

    (fact "Could use the type-repo explicitly"
      (structural-typing.type/built-like mytypes/type-repo :Point ok) => ok)))

(fact "and with invalid - note that these types throw on failure"
  (let [bad {:x 1.5}]
    (mytypes/built-like? :Point bad) => false

    ;; All the text appears in the exceptions message, newline separated.
    ;; Checked independently here for readability
    (mytypes/built-like :Point bad) => (throws #":x should be `integer\?`")
    (mytypes/built-like :Point bad) => (throws #":y does not exist")

    ;; (mytypes/<>built-like bad :Point) => (throws #":x should be.*:y does not exist")
    (mytypes/<>built-like bad :Point) => (throws #":x should be `integer\?`")
    (mytypes/<>built-like bad :Point) => (throws #":y does not exist")

    (mytypes/all-built-like :Point [bad bad]) => (throws #"\[0 :x\] should be")
    (mytypes/all-built-like :Point [bad bad]) => (throws #"\[0 :y\] does not exist")
    (mytypes/all-built-like :Point [bad bad]) => (throws #"\[1 :x\] should be ")
    (mytypes/all-built-like :Point [bad bad]) => (throws #"\[1 :y\] does not exist")


    (mytypes/<>all-built-like [bad bad] :Point) => (throws #"\[0 :x\] should be")
    (mytypes/<>all-built-like [bad bad] :Point) => (throws #"\[0 :y\] does not exist")
    (mytypes/<>all-built-like [bad bad] :Point) => (throws #"\[1 :x\] should be ")
    (mytypes/<>all-built-like [bad bad] :Point) => (throws #"\[1 :y\] does not exist")))

