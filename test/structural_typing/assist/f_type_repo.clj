(ns structural-typing.assist.f-type-repo
  (:require [structural-typing.assist.type-repo :as subject]
            [structural-typing.type :as type]
            [structural-typing.assist.oopsie :as oopsie])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))

(fact "an example case"
  (let [repo (-> (subject/->TypeRepo identity oopsie/explanations)
                 (subject/hold-type :Type [:a]))]
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
                 (subject/hold-type :AB [ (includes :A) (requires :b) {:b string?} ])
                 (subject/replace-error-handler oopsie/explanations))]
    (subject/check-type repo :AB {:b "s"}) => empty?
    (subject/check-type repo :AB {:a 1 :b "s"}) => empty?
    (subject/check-type repo :AB {:a "s"}) => (just (contains {:path [:a]})
                                                    (contains {:path [:b]})
                                                    :in-any-order)))

(future-fact "values of types are not allowed to be nil"
  (let [repo (-> subject/empty-type-repo
                 (subject/hold-type :Unused [ {:b string?} ])
                 (subject/replace-error-handler oopsie/explanations))]
    (subject/check-type repo :Unused nil) => (just (contains {:path []})))

  (fact "empty structures are not misclassified as nil"
    (let [repo (-> subject/empty-type-repo
                   (subject/hold-type :Hash [ {:a even?} ])
                   (subject/hold-type :Vec [ {[type/ALL] even?} ])
                   (subject/replace-error-handler oopsie/explanations))]
      (subject/check-type repo :Hash {}) => []
      (subject/check-type repo :Hash []) => []
      (subject/check-type repo :Vec {}) => []
      (subject/check-type repo :Vec []) => [])))
