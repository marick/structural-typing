(ns monadic
  "Using an Either monad to separate error indicators from valid values"
  (:require [structural-typing.type :as type]
            [structural-typing.global-type :as global-type]
            [blancas.morph.monads :as m])
  (:use midje.sweet))

(global-type/start-over!)
(global-type/set-failure-handler! m/left)
(global-type/set-success-handler! m/right)
(global-type/named! :at-least-a-and-b [:a :b])


(fact "using an Either monad to separate out success from failure cases"
  (let [result (map #(type/checked :at-least-a-and-b %)
                    [{:a 1} {:b 2} {:a 1 :b 2} {:a 1 :b 2 :c 3}])]
    (m/rights result) => [{:a 1 :b 2} {:a 1 :b 2 :c 3}]
    (flatten (m/lefts result)) => [":b must be present and non-nil"
                                   ":a must be present and non-nil"]))

;; The above is rather annoying, since the messages don't say *which* map is
;; missing a particular key. We can update the "map adapter" to prepend the
;; original candidate: 

(require '[structural-typing.pipeline-stages :as pipeline])
(global-type/set-map-adapter!
 (fn [explanation-map candidate]
   ;; Convert bouncer map into explanations sequence
   (let [default (pipeline/default-map-adapter explanation-map candidate)]
     (cons candidate default))))

(fact "using an Either monad"
  (let [result (map #(type/checked :at-least-a-and-b %)
                    [{:c 1} {:a 1} {:b 2} {:a 1 :b 2}])]
    (m/lefts result) => (just [{:c 1} ":a must be present and non-nil" 
                                      ":b must be present and non-nil"]
                              [{:a 1} ":b must be present and non-nil"]
                              [{:b 2} ":a must be present and non-nil"])))

(global-type/start-over!) 
