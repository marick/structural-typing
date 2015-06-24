(ns structural-typing.f-type
  (:require [structural-typing.type :as type])
  (:require [structural-typing.type :as type-repo])
  (:use midje.sweet))

(fact "about checking"
  (let [repo (-> type/empty-type-repo
                 (type/replace-error-handler #(cons :error %))
                 (type/replace-success-handler (constantly "yay"))
                 (type/named :A [:a])
                 (type/named :B [:b]))]
                             
    (fact "calls the error and success function depending on the oopsies it gets back"
      (type/checked repo :A {:a 1}) => "yay"
      (type/checked repo :A {}) => (just :error (contains {:path [:a]})))

    (fact "can take a vector of type signifiers"
      (type/checked repo [:A :B] {:a 1}) => (just :error (contains {:path [:b]}))
      (type/checked repo [:A :B] {:b 1}) => (just :error (contains {:path [:a]}))
      (type/checked repo [:A :B] {:a 1, :b 1}) => "yay")))

(fact "about `instance?`"
  (let [repo (-> type/empty-type-repo
                 (type/named :A [:a])
                 (type/named :B [:b]))]
                             
    (fact "one signifier"
      (type/instance? repo :A {:a 1}) => true
      (type/instance? repo :A {}) => false)

    (fact "can take a vector of type signifiers"
      (type/instance? repo [:A :B] {:a 1}) => false
      (type/instance? repo [:A :B] {:b 1}) => false
      (type/instance? repo [:A :B] {:a 1, :b 1}) => true)))
