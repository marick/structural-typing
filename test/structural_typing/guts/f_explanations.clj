(ns structural-typing.guts.f-explanations
  (:require [structural-typing.guts.explanations :as subject]
            [midje.sweet :refer :all]))

(fact "you can make an oopsie with a by-order-of-arguments explainer"
  (let [make:oopsies (subject/mkfn:structural-singleton-oopsies
                      #(format "path: %s, leaf value: %s" %1 %2)
                      [:path :leaf-value])
        oopsies (make:oopsies {:path [:a :b], :leaf-value 33, :whole-value "whole"})
        [oopsie] oopsies]
    oopsies => (just (contains {:path [:a :b], :leaf-value 33, :whole-value "whole"}))
    ((:explainer oopsie) oopsie) => "path: [:a :b], leaf value: 33"))


(facts "err:bad-range-target"
  (subject/err:bad-range-target '[:a (RANGE 1 2)] {:a 5} 5)
  => "[:a (RANGE 1 2)] is not a path into `{:a 5}`; RANGE cannot make sense of non-collection `5`")

(facts "err:bad-all-target"
  (subject/err:bad-all-target '[:a ALL] {:a 5} 5)
  => "[:a ALL] is not a path into `{:a 5}`; ALL cannot make sense of non-collection `5`"
  (subject/err:bad-all-target '[:a ALL] {:a {:b 5}} {:b 5})
  => "[:a ALL] is not a path into `{:a {:b 5}}`; ALL cannot make sense of map `{:b 5}`")


(facts "err:notpath"
  (subject/err:notpath [:a :k] 5) => "[:a :k] is not a path into `5`"
  (subject/err:notpath [:a :k] []) => "[:a :k] is not a path into `[]`"
  (subject/err:notpath [:a :k] '(1 2 3)) => "[:a :k] is not a path into `(1 2 3)`"
  (subject/err:notpath [:a :k] (map inc (range 0 3))) => "[:a :k] is not a path into `(1 2 3)`"
  (subject/err:notpath [:a :k] "foo") => "[:a :k] is not a path into `\"foo\"`"
  (subject/err:notpath [:a :k] :foo) => "[:a :k] is not a path into `:foo`"
  (subject/err:notpath [:a :k] 'foo) => "[:a :k] is not a path into `foo`")


(future-fact "Hand-test range output - descend into 5, what incomplete range looks like")
(future-fact "finish ALL output so that it chokes on non-sequential collection.")


(future-fact "Add tests for all new explanations")
