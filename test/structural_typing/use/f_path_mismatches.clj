(ns structural-typing.use.f-path-mismatches
  "Cases where no additions to the value could make it match the path"
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))


(future-fact "whole value is nil: impossible"
  (type! :String {[] string?})
  (check-for-explanations :String nil)
  => (just ["Value is nil, and that makes Sir Tony Hoare sad"]))

(future-fact "keywords"
  (type! :X {[:a :b] [required-path even?]})
  (fact "matching non-maps: impossible"
    (check-for-explanations :X 1) =>      (just "x")
    (check-for-explanations :X [:a 1]) => (just "y")
    (check-for-explanations :X {:a 1}) => (just "x"))

  (fact "missing or nil values: truncated"
    (check-for-explanations :X {}) => (just (err:missing :a))
    (check-for-explanations :X {:b 1}) => (just (err:missing :a))
    (check-for-explanations :X {:a nil}) => (just (err:value-nil [:a :b]))
    (check-for-explanations :X {:a {:c 1}}) => (just (err:missing [:a :b]))))

(future-fact "indices"
  (type! :X {[1 0] [required-path even?]})
  (fact "non-collections: impossible"
    (check-for-explanations :X 1) =>      (just "x")
    (check-for-explanations :X [[] 1]) => (just "y"))

  (fact "it is 'impossible' to apply an index to maps or sets"
    (check-for-explanations :X [[] {:a 1}]) => (just "x")
    (check-for-explanations :X [[] #{0 1}]) => (just "y"))

  (fact "missing or nil values: truncated"
    (check-for-explanations :X [     ]) => (just (err:missing [1]))
    (check-for-explanations :X [[]   ]) => (just (err:missing [1]))
    (check-for-explanations :X [[] []]) => (just (err:missing [1 0])))

  (fact "note that you can take the index of an infinite sequence"
    (let [result (built-like :X (repeat (list 2 1)))]
      (sequential? result) => true
      (take 3 result) => (take 3 (repeat (list 2 1))))))

(fact "Two ALLs in a row - interaction with missing and nil"
  (future-fact "better error messages")
  (check-for-explanations {[ALL :x ALL ALL] [even? required-path]}
                          [{:x [[2]]}
                           {:x [[2 1] [3]]}
                           {:x [     ]}])
  =future=> (just "message")
  (check-for-explanations {[ALL :x ALL ALL :z] [even? required-path]}
                          [{:x [[{:z 2}]]}
                           {:x [[{:z 1}]]}
                           {:x [[{:notz 2}]]}
                           {:x [     ]}])
  =future=> (just "message"))


(fact "nested missing or nil values: truncated"
  (type! :X {[:x ALL] [required-path even?]})
  (fact "as before, non-collections: impossible"
    (check-for-explanations :X {:x 1}) =>   (just (err:not-collection [:x ALL] 1)))
  (future-fact "A previously non-indexed element is checked for existence "
    (check-for-explanations :X {}) =>         (just (err:missing :x))
    (check-for-explanations :X {:x nil}) =>   (just (err:value-nil :x)))

  (type! :X {[ALL :x] [required-path even?]})
  (fact "ALL passes a nil along to later elements"
    (check-for-explanations :X [{:x 1} {} {:x nil}]) => (just (err:shouldbe [0 :x] "even?" 1)
                                                              (err:missing [1 :x])
                                                              (err:value-nil [2 :x]))))

    
(fact "RANGE"
  (future-fact "non-collections: impossible"
    (let [path [(RANGE 1 2) (RANGE 1 2)]]
      (type! :X {path [required-path]})
      (check-for-explanations :X 8) =>   (just (err:not-sequential path 8))
      (check-for-explanations :X [0 1]) => (just (err:not-sequential path 1))))
  
  (fact "missing or nil values: truncated"
    (let [path [(RANGE 1 4) (RANGE 1 3)]]
      (type! :X {path [required-path]})
      (let [in [:unchecked [10 11 12] [20 21 22] [30 31 32] :unchecked]]
        (built-like :X in) => in)

      (let [in [ :unchecked [10 11 12] [20 21] [30] :unchecked]]
        (check-for-explanations :X in) => (just (err:missing [2 2])
                                                (err:missing [3 1])
                                                (err:missing [3 2])))

      (let [in [ :unchecked [10 11 12] nil [30 31 32] :unchecked]]
        (check-for-explanations :X in) =future=> (just (err:selector-at-nil [2])))

      (future-fact "in a combination of non-sequential value and truncation, you only see impossible path"
        (let [in [ :unchecked [10 11 12] :oops [30 31 32] :unchecked]]
          (check-for-explanations :X in) => (just (err:not-sequential path :oops))))))

  (fact "completely empty arrays count as truncation"
    (let [path [(RANGE 1 2) (RANGE 1 3)]]
      (type! :X {path [required-path]})
      (fact "top level"
        (check-for-explanations :X []) => (just (err:missing [1]))
      (future-fact "nested"
        (check-for-explanations :X [ [] ]) => (just (err:missing [1])
                                                    (err:missing [1]))
        (check-for-explanations :X [ [:ignored] [] [:ignored] ]) => (just (err:missing [1 1])
                                                                          (err:missing [1 2])))))

  (fact "note that nils are allowed when there is no required path"
    (let [path [(RANGE 1 2) (RANGE 1 3)]]
      (type! :X {path [even?]})
      (fact "top level"
        (built-like :X []) => []
      (fact "nested"
        (built-like :X [ [] ]) => [[]]
        (built-like :X [ [:ignored] [] [:ignored]]) => [[:ignored] [] [:ignored]]))))

  (fact "cannot take maps or sets"
    (type! :X (requires [(RANGE 1 2)]))
    (let [bad {:a 1, :b 2, :c 3}]
      (check-for-explanations :X bad) =future=> (just (err:not-sequential [1] bad)))

    (let [bad #{1 2 3 4 5}]
      (check-for-explanations :X bad) => (just (err:not-sequential [(RANGE 1 2)] bad)))))

(future-fact "a path predicate that blows up counts as an impossible path"
  (type! :X {[:a pos?] even?})
  (check-for-explanations :X {:a "string"}) => (just :notpath)))


(fact "Some random leftover tests"
  (type! :Points {[ALL :x] integer?
                  [ALL :y] integer?})
  (check-for-explanations :Points 3) =future=> (just (err:not-collection [ALL] 3))

  (check-for-explanations :Points [1 2 3]) =future=> (just "x" "y")

  (future-fact "works for partial collections"
    (type! :Figure (requires :color [:points ALL (each-of :x :y)]))
    (check-for-explanations :Figure {:points 3})
    => (just (err:missing :color)
             "x"
             "y"))

  (future-fact "ALL following ALL"
    (type! :Nesty {[:x ALL ALL :y] integer?})
    (check-for-explanations :Nesty {:x [1]}) => "x"))
