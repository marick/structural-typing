(ns structural-typing.use.condensed-type-descriptions.f-explain-with
  (:require [structural-typing.assist.oopsie :as oopsie])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil)
  (:refer-clojure :except [any?]))


(start-over!)

(def good-password? (->> (partial re-find #"\d")
                         (explain-with 
                          (fn [oopsie] 
                            (format "%s should contain a digit" 
                                    (oopsie/friendly-path oopsie))))))

(type! :Account {:password good-password?})
(fact
  (with-out-str (built-like :Account {:password "foo"}))
  => #":password should contain a digit")

(start-over!)
