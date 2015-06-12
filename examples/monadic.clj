(ns monadic
  (:require [structural-typing.type :as type]
            [structural-typing.global-type :as global-type]
            [blancas.morph.monads :as m])
  (:use midje.sweet))

(global-type/start-over!)
(global-type/set-failure-handler! m/left)
(global-type/set-success-handler! m/right)
(global-type/named! :ab [:a :b])

(fact "using an Either monad"
  (let [result (map #(type/checked :ab %) [{:a 1} {:b 2} {:a 1 :b 2} {:a 1 :b 2 :c 3}])]
    (m/rights result) => [{:a 1 :b 2} {:a 1 :b 2 :c 3}]
    (flatten (m/lefts result)) => (just #"b must be present" #"a must be present")))


(global-type/set-map-adapter! (fn [errors-by-key original]
                                (cons original (flatten (vals errors-by-key)))))

(fact "using an Either monad"
  (let [result (map #(type/checked :ab %) [{:a 1} {:b 2} {:a 1 :b 2} {:a 1 :b 2 :c 3}])]
    (m/lefts result) => [[{:a 1} ":b must be present and non-nil"]
                         [{:b 2} ":a must be present and non-nil"]]))

(global-type/start-over!) 
