(ns f-check-redefs
  "Check that all the exported vars actually work"
  (:require [monadic-define-1 :as mv1]
            [monadic-define-2 :as mv2]
            [timbre-define-1 :as tv1]
            [timbre-define-2 :as tv2]
            [blancas.morph.core :as mc]
            [blancas.morph.monads :as m]
            [clojure.math.numeric-tower :as math])
  (:use midje.sweet
        structural-typing.clojure.core)
  (:refer-clojure :except [any?]))


;; Note: in this version of Morph, two Rights with the same content do not count as equal.

(fact "monadic-define-1"
  (let [point {:x 1 :y 1}
        nopoint {}]
    (m/run-right (mv1/built-like :Point point)) => point
    (m/run-right (mv1/all-built-like :Point [point])) => [point]
    (m/run-right (mv1/<>built-like point :Point)) => point
    (m/run-right (mv1/<>all-built-like [point] :Point)) => [point]
    (mv1/built-like? :Point point) => true

    (mv1/built-like :Point nopoint) => m/left?
    (mv1/all-built-like :Point [nopoint]) => m/left?
    (mv1/<>built-like nopoint :Point) => m/left?
    (mv1/<>all-built-like [nopoint] :Point) => m/left?
    (mv1/built-like? :Point nopoint) => false))


(fact "monadic-define-2"
  (let [point {:x 2 :y 2}
        nopoint {}]
    (m/run-right (mv2/built-like :Point point)) => point
    (m/run-right (mv2/all-built-like :Point [point])) => [point]
    (m/run-right (mv2/<>built-like point :Point)) => point
    (m/run-right (mv2/<>all-built-like [point] :Point)) => [point]
    (mv2/built-like? :Point point) => true

    (mv2/built-like :Point nopoint) => m/left?
    (mv2/all-built-like :Point [nopoint]) => m/left?
    (mv2/<>built-like nopoint :Point) => m/left?
    (mv2/<>all-built-like [nopoint] :Point) => m/left?
    (mv2/built-like? :Point nopoint) => false))

(fact "timbre-define-1"
  (let [point {:x 1 :y 1}
        nopoint {}]
    (tv1/built-like :Point point) => point
    (tv1/all-built-like :Point [point]) => [point]
    (tv1/<>built-like point :Point) => point
    (tv1/<>all-built-like [point] :Point) => [point]
    (tv1/built-like? :Point point) => true

    (val-and-output (tv1/built-like :Point nopoint)) => (just nil anything)
    (val-and-output (tv1/all-built-like :Point [nopoint])) => (just nil anything)
    (val-and-output (tv1/<>built-like nopoint :Point)) => (just nil anything)
    (val-and-output (tv1/<>all-built-like [nopoint] :Point)) => (just nil anything)
    (tv1/built-like? :Point nopoint) => false))

(fact "timbre-define-2"
  (let [point {:x 2 :y 2}
        nopoint {}]
    (tv2/built-like :Point point) => point
    (tv2/all-built-like :Point [point]) => [point]
    (tv2/<>built-like point :Point) => point
    (tv2/<>all-built-like [point] :Point) => [point]
    (tv2/built-like? :Point point) => true

    (val-and-output (tv2/built-like :Point nopoint)) => (just nil anything)
    (val-and-output (tv2/all-built-like :Point [nopoint])) => (just nil anything)
    (val-and-output (tv2/<>built-like nopoint :Point)) => (just nil anything)
    (val-and-output (tv2/<>all-built-like [nopoint] :Point)) => (just nil anything)
    (tv2/built-like? :Point nopoint) => false))

