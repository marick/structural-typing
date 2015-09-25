(ns structural-typing.assist.f-type-repo
  (:require [structural-typing.assist.type-repo :as subject]
            [structural-typing.type :as type]
            [structural-typing.assist.oopsie :as oopsie])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))

(defn check-type [type-repo type-signifier candidate]
  ((subject/get-type type-repo type-signifier) candidate))

(defn oopsie-with-path [path]
  (fn [actual]
    ((contains {:path path}) actual)))

(fact "an example case"
  (let [repo (-> (subject/->TypeRepo identity oopsie/explanations)
                 (subject/hold-type :Type [:a]))]
    (check-type repo :Type {:b 1}) => (just (oopsie-with-path [:a]))
    (check-type repo :Type {:a 1}) => empty?))

(fact "there is an empty type repo"
  (let [repo (-> subject/empty-type-repo
                 (subject/hold-type :Type [ {:a integer?} ])
                 (subject/replace-error-handler oopsie/explanations))]
    (check-type repo :Type {:b 1}) => empty?
    (check-type repo :Type {:a 1}) => empty?
    (check-type repo :Type {:a "string"}) => (just (oopsie-with-path [:a]))))
    
(fact "using previously-defined types"
  (let [repo (-> subject/empty-type-repo
                 (subject/hold-type :A [ {:a integer?} ])
                 (subject/hold-type :AB [ (includes :A) (requires :b) {:b string?} ])
                 (subject/replace-error-handler oopsie/explanations))]
    (check-type repo :AB {:b "s"}) => empty?
    (check-type repo :AB {:a 1 :b "s"}) => empty?
    (check-type repo :AB {:a "s"}) => (just (oopsie-with-path [:a])
                                            (oopsie-with-path [:b]))))
