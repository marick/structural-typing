(ns monadic-use
  "Using an Either monad to separate mistyped from valid values"
  (:require [monadic-define-1 :as v1]
            [monadic-define-2 :as v2]
            [blancas.morph.core :as mc]
            [blancas.morph.monads :as m]
            [clojure.math.numeric-tower :as math])
  (:use midje.sweet))


;;; Note: Some of the tests are a little awkward because Morph doesn't
;;; consider `(left 5)` to be equal to `(left 5)`.

(fact "successful values are wrapped in a `Right`"
  (let [result (v1/built-like :Point {:x 1, :y 1})]
    result => m/right?
    (m/run-right result) => {:x 1, :y 1}))

(fact "errors produce a `Left` containing the error messages"
  (let [result (v1/built-like :Point {:x 1})]
    result => m/left?
    (m/run-left (v1/built-like :Point {:x 1})) => [":y does not exist"]))

(fact "`all-built-like` is used to reduce a collection of structures into a Left or Right"
  (fact "success wraps the original in a Right"
    (let [result (v1/all-built-like :Point [{:x 1, :y 1}, {:x 2 :y 2}])]
      result => m/right?
      (m/run-right result) => [{:x 1, :y 1}, {:x 2 :y 2}]))

  (fact "failure wraps error messages in a Left. Note the indices"
    (let [result (v1/all-built-like :Point [{:x 1} {:y 2} {:x 1 :y 2}
                                            {:x 1 :y 2 :color "red"} {:x "1"}])]
      result => m/left?
      (m/run-left result) => (just "[0 :y] does not exist"
                                   "[1 :x] does not exist"
                                   "[4 :x] should be `integer?`; it is `\"1\"`"
                                   "[4 :y] does not exist"))))



(fact "using an Either monad to separate out success from failure cases"
  (let [input [{:x 1} {:y 2} {:x 1 :y 2} {:x 1 :y 2 :color "red"} {:x "1"}]
        result (map #(v1/built-like :Point %) input)]
    (fact "filtering out the Right elements of the sequence works nicely"
      (m/rights result) => [{:x 1 :y 2} {:x 1 :y 2 :color "red"}])

    (fact "It is awkward, though, that the collected Lefts don't have indexes"
      (flatten (m/lefts result)) => (just ":y does not exist"
                                          ":x does not exist"
                                          ":x should be `integer?`; it is `\"1\"`"
                                          ":y does not exist"))))

;;;; Version 2 helps with the awkwardness in the previous test by
;;;; adding the structure itself to the error message.

(fact "version 2 identifies the source candidate"
  (let [result (map #(v2/built-like :Point %)
                    [{:x 1} {:y 2} {:x 1 :y 2} {:x 1 :y 2 :color "red"} {:x "1"}])]
    (m/rights result) => [{:x 1 :y 2} {:x 1 :y 2 :color "red"}]
    (nth (m/lefts result) 0) => [{:x 1} ":y does not exist"]
    (nth (m/lefts result) 1) => [{:y 2} ":x does not exist"]
    (nth (m/lefts result) 2) => (just {:x "1"}
                                      ":x should be `integer?`; it is `\"1\"`"
                                      ":y does not exist")))


;;; Just for yucks, an example of Morph's equivalent of Haskell's `do`:

(def right m/right)
(def wrong m/left)
(def square #(math/expt % 2))

(defn hypotenuse-length [{:keys [x y]}]
  (math/sqrt (+ (square x) (square y))))

(fact "an example of sequencing"
  (m/run-right
   (mc/monad [origin-triangle (v2/built-like :OriginTriangle {:x 3, :y 4})]
     (right (hypotenuse-length origin-triangle))))
  => 5

  (mc/monad [origin-triangle (v2/built-like :OriginTriangle {:x 0, :y 4})]
    (right (hypotenuse-length origin-triangle)))
  => m/left?)
