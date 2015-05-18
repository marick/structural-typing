(ns structural-typing.f-monad
  (:require [structural-typing.type :as type]
            [blancas.morph.monads :as m])
  (:use midje.sweet))

(type/start-over!)
(type/set-failure-handler! m/left)
(type/set-success-handler! m/right)
(type/named! :ab [:a :b])

(fact "using an Either monad"
  (let [result (map #(type/checked :ab %) [{:a 1} {:b 2} {:a 1 :b 2} {:a 1 :b 2 :c 3}])]
    (m/rights result) => [{:a 1 :b 2} {:a 1 :b 2 :c 3}]
    (flatten (m/lefts result)) => ["b must be present" "a must be present"]))


(type/set-formatter! (fn [errors-by-key original]
                       (cons original (flatten (vals errors-by-key)))))

(fact "using an Either monad"
  (let [result (map #(type/checked :ab %) [{:a 1} {:b 2} {:a 1 :b 2} {:a 1 :b 2 :c 3}])]
    (m/lefts result) => [[{:a 1} "b must be present"]
                         [{:b 2} "a must be present"]]))
