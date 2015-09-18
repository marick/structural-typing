(ns structural-typing.use.condensed-type-descriptions.f-whole-type-and-implies
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)


(fact "a predicate is shorthand for a whole-type check"
  (fact "the expanded form"
    (type! :String {[] string?})
    (checked :String "foo") => "foo"
    (check-for-explanations :String nil) => (just ["Value is nil, and that makes Sir Tony Hoare sad"])
    (check-for-explanations :String 1) => (just [(err:shouldbe "Value" "string?" 1)]))

  (fact "the predicate form"
    (type! :String string?)
    (checked :String "foo") => "foo"
    (check-for-explanations :String nil) => (just ["Value is nil, and that makes Sir Tony Hoare sad"])
    (check-for-explanations :String 1) => (just [(err:shouldbe "Value" "string?" 1)]))

  (fact "a more elaborate check"
    (type! :X map? (show-as "even size" (comp even? count)))
    (checked :X {:a 1, :b "foo"}) => {:a 1, :b "foo"}
    (check-for-explanations :X {:b 1}) => (just (err:shouldbe "Value" "even size" {:b 1}))
    (check-for-explanations :X [1 2]) => (just (err:shouldbe "Value" "map?" [1 2]))))




(fact "`implies` is typically used for checking the whole type"
  (fact "easily used to check that the presence of one key implies the presence of another"
    (type! :X (pred/implies :a :b))
    
    (checked :X {:a 2, :b 1}) => {:a 2, :b 1}
    (check-for-explanations :X {:a 2}) => [(err:required :b)]
    (checked :X {:b 2}) => {:b 2})

  (fact "going both ways"
    (type! :X (pred/implies :a :b)
              (pred/implies :b :a))

    (checked :X {:a 2, :b 1}) => {:a 2, :b 1}
    (checked :X {}) => {}
    (check-for-explanations :X {:a 2}) => [(err:required :b)]
    (check-for-explanations :X {:b 2}) => [(err:required :a)])

  (fact "multiple paths can be checked"
    (type! :X (pred/implies :a (requires :b [:c :d])))
    
    (check-for-explanations :X {:a 2, :b 1}) => [(err:required [:c :d])]
    (let [in {:a 2, :b 1 :c {:d 3}}]
      (checked :X in) => in)
    (checked :X {:b 2}) => {:b 2})




  (fact "can also check that the presence of one key implies the nonexistence of another"
    (type! :X (pred/implies :a (show-as "missing :b" (complement :b)))
              (pred/implies :b (show-as "missing :a" (complement :a))))

    (let [in {:a 2, :b 1}]
      (check-for-explanations :X in) => (just (err:shouldbe "Value" "missing :a" in)
                                              (err:shouldbe "Value" "missing :b" in)))
                                       
    (checked :X {}) => {}
    (checked :X {:a 2}) => {:a 2}
    (checked :X {:b 2}) => {:b 2})


  (fact "presence of a key can cause a test of another key"
    (type! :X (pred/implies :a {:b even?}))
    (checked :X {:a 1}) => {:a 1}
    (check-for-explanations :X {:a 1 :b 1}) => [(err:shouldbe :b "even?" 1)])
  
  (fact "a variant of the above that requires :b"
    (type! :X (pred/implies :a {:b [required-key even?]}))
    (check-for-explanations :X {:a 1}) => [(err:required :b)]
    (check-for-explanations :X {:a 1 :b 1}) => [(err:shouldbe :b "even?" 1)])
  

  (fact "a variant that supports multiple condensed descriptions"
    (type! :X (pred/implies :a (pred/all-of :b {:b even?})))
    (check-for-explanations :X {:a 1}) => [(err:required :b)]
    (check-for-explanations :X {:a 1 :b 1}) => [(err:shouldbe :b "even?" 1)])


  (fact "an implication that checks the whole structure"
    (type! :X (pred/implies coll? (show-as "even" (comp even? count)))
              (pred/implies string? (show-as "uncool" #(not= % "cool"))))
    (checked :X [1 2]) => [1 2]
    (check-for-explanations :X [2]) => [(err:shouldbe "Value" "even" [2])]

    (checked :X "something uncool") => "something uncool"
    (check-for-explanations :X "cool") => [(err:shouldbe "Value" "uncool" "\"cool\"")]

    (checked :X 1) => 1)
  )
  
(start-over!)

