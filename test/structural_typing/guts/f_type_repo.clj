(ns structural-typing.guts.f-type-repo
  (:require [structural-typing.guts.type-repo :as subject]
            [structural-typing.pred-writing.shapes.oopsie :as oopsie])
  (:require [structural-typing.guts.paths.substituting :refer [includes]])
  (:use midje.sweet))

(fact "an example case (most are in the examples dir"
  (let [repo (-> (subject/->TypeRepo identity oopsie/explanations)
                 (subject/hold-type :Type [ [:a] ]))]
    (subject/check-type repo :Type {:b 1}) => (just (contains {:path [:a]}))
    (subject/check-type repo :Type {:a 1}) => empty?))

(fact "there is an empty type repo"
  (let [repo (-> subject/empty-type-repo
                 (subject/hold-type :Type [ {:a integer?} ])
                 (subject/replace-error-handler oopsie/explanations))]
    (subject/check-type repo :Type {:b 1}) => empty?
    (subject/check-type repo :Type {:a 1}) => empty?
    (subject/check-type repo :Type {:a "string"}) => (just (contains {:path [:a]}))))
    
(fact "using previously-defined types"
  (let [repo (-> subject/empty-type-repo
                 (subject/hold-type :A [ {:a integer?} ])
                 (subject/hold-type :AB [ (includes :A) [:b] {:b string?} ])
                 (subject/replace-error-handler oopsie/explanations))]
    (subject/check-type repo :AB {:b "s"}) => empty?
    (subject/check-type repo :AB {:a 1 :b "s"}) => empty?
    (subject/check-type repo :AB {:a "s"}) => (just (contains {:path [:a]})
                                                 (contains {:path [:b]})
                                                 :in-any-order)))