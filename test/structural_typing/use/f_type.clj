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
      (type/checked repo :A {:a 1}) => "yay"
      (type/checked repo :A {}) => (just :error (contains {:path [:a]})))

    (fact "can take a vector of type signifiers"
      (type/checked repo [:A :B] {:a 1}) => (just :error (contains {:path [:b]}))
      (type/checked repo [:A :B] {:b 1}) => (just :error (contains {:path [:a]}))
      (type/checked repo [:A :B] {:a 1, :b 1}) => "yay")

    (future-fact "the vector can contain on-the-fly condensed type descriptions"
      (let [path [:A (requires :c) {:a even?}]]
        (type/checked repo path {:a 2, :c 1}) => {:a 2, :c 1}
        (check-for-explanations {:c 1}) => (just (err:required :a))
        (check-for-explanations {:a 1}) => (just (err:shouldbe :a "even?" 1)
                                                 (err:required :c)))))

  (fact "values of types are not allowed to be nil"
    (let [repo (-> type/empty-type-repo
                   (type/named :Unused {:b string?}))]
      (check-for-explanations repo :Unused nil) => (just #"Value is nil"))
    
    (fact "empty structures are not misclassified as nil"
      (let [repo (-> type/empty-type-repo
                     (type/named :Hash {:a even?})
                     (type/named :Vec {[type/ALL] even?}))]
        (type/checked repo :Hash {}) => {}
        (type/checked repo :Vec []) => vector?
        (type/checked repo :Vec []) => []))))
                                                 
      
      

(fact "about `described-by?`"
  (let [repo (-> type/empty-type-repo
                 (type/named :A (requires :a))
                 (type/named :B (requires :b)))]
                             
    (fact "one signifier"
      (type/described-by? repo :A {:a 1}) => true
      (type/described-by? repo :A {}) => false)

    (fact "can take a vector of type signifiers"
      (type/described-by? repo [:A :B] {:a 1}) => false
      (type/described-by? repo [:A :B] {:b 1}) => false
      (type/described-by? repo [:A :B] {:a 1, :b 1}) => true)))



(fact "imported preds"
  (let [repo (-> type/empty-type-repo
                 (type/named :Member {:a (pred/member [1 2 3])})
                 (type/named :Exactly {:a (pred/exactly even?)}))]
    (type/checked repo :Member {:a 3}) => {:a 3}
    (type/checked repo :Exactly {:a even?}) => {:a even?}))



(fact "degenerate cases"
  (fact "a predicate list can be empty"
    (let [repo (-> type/empty-type-repo
                   (type/named :A {:a []}))]
      (type/checked repo :A {})
      (type/checked repo :A {:a 1})))

  (fact "a whole type map can be empty"
    (let [repo (-> type/empty-type-repo
                   (type/named :A {}))]
      (type/checked repo :A {})
      (type/checked repo :A {:a 1}))))

    

(fact "types can be strings"
  (let [repo (-> type/empty-type-repo
                 (type/named "A" {:a even?}))]
    (type/checked repo "A" {:a 2}) => {:a 2}))
