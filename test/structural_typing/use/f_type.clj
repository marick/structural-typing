(ns structural-typing.use.f-type
  (:require [structural-typing.type :as type]
            [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))

(fact "about checking"
  (let [repo (-> type/empty-type-repo
                 (type/replace-error-handler #(cons :error %))
                 (type/replace-success-handler (constantly "yay"))
                 (type/named :A (requires :a))
                 (type/named :B (requires :b)))]
                             
    (fact "calls the error and success function depending on the oopsies it gets back"
      (type/built-like repo :A {:a 1}) => "yay"
      (type/built-like repo :A {}) => (just :error (contains {:path [:a]})))

    (fact "can take a vector of type signifiers"
      (type/built-like repo [:A :B] {:a 1}) => (just :error (contains {:path [:b]}))
      (type/built-like repo [:A :B] {:b 1}) => (just :error (contains {:path [:a]}))
      (type/built-like repo [:A :B] {:a 1, :b 1}) => "yay")

    (fact "the vector can contain on-the-fly condensed type descriptions"
      (let [path [:A (requires :c) {:a even?}]]
        (type/built-like repo path {:a 2, :c 1}) => "yay"
        (type/built-like repo path {:c 1}) => (just :error (contains {:path [:a]}))
        (type/built-like repo path {:a 1}) => (just :error
                                                 (contains {:path [:c]})
                                                 (contains {:path [:a] :leaf-value 1})
                                                 :in-any-order))))
                                                 

  (fact "values of types are not allowed to be nil"
    (let [repo (-> type/empty-type-repo
                   (type/named :Unused {:b string?}))]
      (check-for-explanations repo :Unused nil) =future=> (just #"Value is nil"))
    
    (fact "empty structures are not misclassified as nil"
      (let [repo (-> type/empty-type-repo
                     (type/named :Hash {:a even?})
                     (type/named :Vec {[type/ALL] even?}))]
        (type/built-like repo :Hash {}) => {}
        (type/built-like repo :Vec []) => vector?
        (type/built-like repo :Vec []) => []))))
                                                 
(fact "variants of `built-like`"
  (let [repo (-> type/empty-type-repo
                 (type/named :X (requires :x)))]
    (fact "<>built-like"
      (type/<>built-like {:x 1} repo :X) => (type/built-like repo :X {:x 1})
      (with-out-str (type/<>built-like {:notx 1} repo :X))
      => (with-out-str (type/built-like repo :X {:notx 1})))

    (fact "all-built-like"
      (type/all-built-like repo :X [{:x 1} {:x 2}]) => [{:x 1} {:x 2}]
      (type/all-built-like repo :X nil) => nil?
      (type/all-built-like repo :X []) => []
      
      (check-all-for-explanations repo :X [{:x 1} {:b 2}]) => (just (err:missing [1 :x])))

    (fact "<>all-built-like"
      (type/<>all-built-like [{:x 1}] repo :X) => (type/all-built-like repo :X [{:x 1}])
      (with-out-str (type/<>all-built-like [{:notx 1}] repo :X))
      => (with-out-str (type/all-built-like repo :X [{:notx 1}])))))

(fact "about `built-like?`"
  (let [repo (-> type/empty-type-repo
                 (type/named :A (requires :a))
                 (type/named :B (requires :b)))]
                             
    (fact "one signifier"
      (type/built-like? repo :A {:a 1}) => true
      (type/built-like? repo :A {}) => false)

    (fact "can take a vector of type signifiers"
      (type/built-like? repo [:A :B] {:a 1}) => false
      (type/built-like? repo [:A :B] {:b 1}) => false
      (type/built-like? repo [:A :B] {:a 1, :b 1}) => true)))



(fact "imported preds"
  (let [repo (-> type/empty-type-repo
                 (type/named :Member {:a (pred/member [1 2 3])})
                 (type/named :Exactly {:a (pred/exactly even?)}))]
    (type/built-like repo :Member {:a 3}) => {:a 3}
    (type/built-like repo :Exactly {:a even?}) => {:a even?}))



(fact "degenerate cases"
  (fact "a predicate list can be empty"
    (let [repo (-> type/empty-type-repo
                   (type/named :A {:a []}))]
      (type/built-like repo :A {})
      (type/built-like repo :A {:a 1})))

  (fact "a whole type map can be empty"
    (let [repo (-> type/empty-type-repo
                   (type/named :A {}))]
      (type/built-like repo :A {})
      (type/built-like repo :A {:a 1}))))

    

(fact "types can be strings"
  (let [repo (-> type/empty-type-repo
                 (type/named "A" {:a even?}))]
    (type/built-like repo "A" {:a 2}) => {:a 2}))

(fact "An empty type description always succeeds"
  (let [repo (type/named type/empty-type-repo :X)]
    (type/built-like repo :X {:a 2}) => {:a 2}
    
    (future-fact "except that nil is still objected to"
      (check-for-explanations repo :X nil) => (just #"Value is nil"))))

