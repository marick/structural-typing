(ns structural-typing.docs.f-wiki-predicates
 (:require [structural-typing.type :refer :all]
           [structural-typing.global-type :refer :all]
           [structural-typing.api.oopsie :as oopsie]
           [clojure.string :as str])
 (:use [midje.sweet :exclude [exactly]]))

(start-over!)

(type! :Address 
       {:name (show-as "short string" #(<= % 10))})

(fact
  (with-out-str (checked :Address {:name "llllllllllllllllll"}))
  => #":name should be `short string`")

(def good-password? (partial re-find #"\d"))
(type! :Account {:password good-password?})
(fact
  (with-out-str (checked :Account {:password "fool"}))
  => #":password should be `<custom-predicate>`; it is `\"fool\"`")

(def good-password? (->> (partial re-find #"\d")
                         (explain-with 
                          (fn [oopsie] 
                            (format "%s should contain a digit" 
                                    (oopsie/friendly-path oopsie))))))

(type! :Account {:password good-password?})
(fact
  (with-out-str (checked :Account {:password "foo"}))
  => #":password should contain a digit")

(start-over!)
