(ns structural-typing.f-global-type
  (:use [structural-typing type global-type])
  (:use [midje.sweet :exclude [exactly]]))


(start-over!)
(on-error! (partial cons :error))
(on-success! (constantly "yay"))
(type! :A [:a])
(type! :B [:b])
  
(fact "checking"                           
  (fact "calls the error and success function depending on the oopsies it gets back"
    (checked :A {:a 1}) => "yay"
    (checked :A {}) => (just :error (contains {:path [:a]})))

  (fact "can take a vector of type signifiers"
    (checked [:A :B] {:a 1}) => (just :error (contains {:path [:b]}))
    (checked [:A :B] {:b 1}) => (just :error (contains {:path [:a]}))
    (checked [:A :B] {:a 1, :b 1}) => "yay"))

(start-over!)
(type! :A [:a])
(type! :B [:b])
  
(fact "about `described-by?`"
  (fact "one signifier"
    (described-by? :A {:a 1}) => true
    (described-by? :A {}) => false)
  
  (fact "can take a vector of type signifiers"
    (described-by? [:A :B] {:a 1}) => false
    (described-by? [:A :B] {:b 1}) => false
    (described-by? [:A :B] {:a 1, :b 1}) => true))


(start-over!)
