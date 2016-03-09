(ns structural-typing.guts.f-explanations
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.explanations :as subject]
            [structural-typing.guts.exval :refer [->ExVal]]
            [structural-typing.guts.compile.compile-path :refer [ALL RANGE]]
            [midje.sweet :refer :all]))

(fact "you can make an oopsie with a by-order-of-arguments explainer"
  (let [make:oopsies (subject/mkfn:structural-singleton-oopsies
                      #(format "path: %s, leaf value: %s" %1 %2)
                      [:path :leaf-value])
        oopsies (make:oopsies {:path [:a :b], :leaf-value 33, :whole-value "whole"})
        [oopsie] oopsies]
    oopsies => (just (contains {:path [:a :b], :leaf-value 33, :whole-value "whole"}))
    ((:explainer oopsie) oopsie) => "path: [:a :b], leaf value: 33"))


(defn run [f leaf-value path whole-value]
  (let [oopsie (only (f (->ExVal leaf-value path whole-value)))]
    ((:explainer oopsie) oopsie)))

(fact "not-maplike"
  (run subject/oopsies:not-maplike :key [:a :b] {:a :key})
  => "[:a :b] encountered `:key` when a map or record was expected"
  (run subject/oopsies:not-maplike even? [:a] :key)
  => ":a encountered the function `even?` when a map or record was expected")

(fact "not-collection"
  (run subject/oopsies:not-collection "string" [ALL] "string")
  => "[ALL] encountered `\"string\"` when a collection was expected"
  (run subject/oopsies:not-collection :key [(RANGE 1 2)] :key)
  => "[(RANGE 1 2)] encountered `:key` when a collection was expected")

(fact "not-sequential"
  (run subject/oopsies:not-sequential {:b 1} [:a (RANGE 1 2)] {:a {:b 1}})
  => "[:a (RANGE 1 2)] encountered `{:b 1}` when a sequential collection was expected"
  (run subject/oopsies:not-sequential 1990 [0] 1990)
  => "[0] encountered `1990` when a sequential collection was expected")

(fact "incorrectly maplike"
  ;; This is a special case because one could think that `ALL` should treat a
  ;; map as a collection of pairs.
  (run subject/oopsies:maplike {:a 1} [:a ALL] {:a {:a 1}})
  => "[:a ALL] encountered map or record `{:a 1}`; ALL doesn't allow that")

(fact "selector-at-nil"
  (run subject/oopsies:selector-at-nil nil [:a ALL] {:a nil})
  => "[:a ALL] finds a `nil` at the position of the last component"
  (run subject/oopsies:selector-at-nil nil [0] nil)
  => "[0] should not descend into `nil`")

(fact "whole-value-nil"
  (run subject/oopsies:whole-value-nil nil [ALL] nil)
  => "The whole value should not be `nil`")

(fact "value-nil"
  (run subject/oopsies:value-nil nil [:a] {:a nil})
  => ":a has a `nil` value")

(fact "missing"
  (run subject/oopsies:missing nil [:a] {:b 1})
  => ":a does not exist"

  (run subject/oopsies:missing nil [:a (RANGE 3 4)] {:a [1]})
  => "[:a (RANGE 3 4)] does not exist"

  (run subject/oopsies:missing nil [:a 5] {:a [1]})
  => "[:a 5] does not exist")
