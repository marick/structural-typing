(ns structural-typing.use.f-global-type
  "Some basic tests of global-types code"
  (:use [structural-typing type global-type])
  (:use midje.sweet
        structural-typing.assist.testutil))

  


(start-over!)
(on-error! (partial cons :error))
(on-success! (constantly "yay"))
(type! :A (requires :a))
(type! :B (requires :b))
  
(fact "checking"                           
  (fact "calls the error and success function depending on the oopsies it gets back"
    (built-like :A {:a 1}) => "yay"
    (built-like :A {}) => (just :error (contains {:path [:a]})))

  (fact "can take a vector of type signifiers"
    (built-like [:A :B] {:a 1}) => (just :error (contains {:path [:b]}))
    (built-like [:A :B] {:b 1}) => (just :error (contains {:path [:a]}))
    (built-like [:A :B] {:a 1, :b 1}) => "yay"))

(fact "variants of `built-like`"
  (start-over!)
  (type! :X (requires :x))

  (fact "<>built-like"
    (<>built-like {:x 1} :X) => (built-like :X {:x 1})
    (with-out-str (<>built-like {:notx 1} :X))
    => (with-out-str (built-like :X {:notx 1})))
  
  (fact "all-built-like"
    (all-built-like :X [{:x 1} {:x 2}]) => [{:x 1} {:x 2}]
    (all-built-like :X nil) => nil?
    (all-built-like :X []) => []
    
    (check-all-for-explanations :X [{:x 1} {:b 2}]) => (just (err:missing [1 :x])))
  
  (fact "<>all-built-like"
    (<>all-built-like [{:x 1}] :X) => (all-built-like :X [{:x 1}])
    (with-out-str (<>all-built-like [{:notx 1}] :X))
    => (with-out-str (all-built-like :X [{:notx 1}]))))



(start-over!)
(type! :A (requires :a))
(type! :B (requires :b))
  
(fact "about `built-like?`"
  (fact "one signifier"
    (built-like? :A {:a 1}) => true
    (built-like? :A {}) => false)
  
  (fact "can take a vector of type signifiers"
    (built-like? [:A :B] {:a 1}) => false
    (built-like? [:A :B] {:b 1}) => false
    (built-like? [:A :B] {:a 1, :b 1}) => true))


(start-over!)
