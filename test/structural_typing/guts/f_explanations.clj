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


(future-fact "Add tests for all new explanations")
