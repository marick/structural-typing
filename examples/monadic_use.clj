(ns monadic-use
  "Using an Either monad to separate mistyped from valid values"
  (:require [monadic-define-1 :as v1]
            [monadic-define-2 :as v2]
            [blancas.morph.core :as mc]
            [blancas.morph.monads :as m]
            [clojure.math.numeric-tower :as math])
  (:use midje.sweet))

(fact "using an Either monad to separate out success from failure cases"
  (let [result (map #(v1/checked :Point %)
                    [{:x 1} {:y 2} {:x 1 :y 2} {:x 1 :y 2 :color "red"} {:x "1"}])]
    (m/rights result) => [{:x 1 :y 2} {:x 1 :y 2 :color "red"}]
    (flatten (m/lefts result)) => (just ":y must exist and be non-nil"
                                        ":x must exist and be non-nil"
                                        ":y must exist and be non-nil"
                                        ":x should be `integer?`; it is `\"1\"`"
                                        :in-any-order)))

(fact "version 2 identifies the source candidate"
  (let [result (map #(v2/checked :Point %)
                    [{:x 1} {:y 2} {:x 1 :y 2} {:x 1 :y 2 :color "red"} {:x "1"}])]
    (m/rights result) => [{:x 1 :y 2} {:x 1 :y 2 :color "red"}]
    (nth (m/lefts result) 0) => [{:x 1} ":y must exist and be non-nil"]
    (nth (m/lefts result) 1) => [{:y 2} ":x must exist and be non-nil"]
    (nth (m/lefts result) 2) => (just {:x "1"}
                                      ":y must exist and be non-nil"
                                      ":x should be `integer?`; it is `\"1\"`"
                                      :in-any-order)))

(def right m/right)
(def wrong m/left)
(def square #(math/expt % 2))

(defn hypotenuse-length [{:keys [x y]}]
  (math/sqrt (+ (square x) (square y))))

(fact "an example of sequencing"
  (m/run-right
   (mc/monad [origin-triangle (v2/checked :OriginTriangle {:x 3, :y 4})]
     (right (hypotenuse-length origin-triangle))))
  => 5

  (mc/monad [origin-triangle (v2/checked :OriginTriangle {:x 0, :y 4})]
    (right (hypotenuse-length origin-triangle)))
  => m/left?)
