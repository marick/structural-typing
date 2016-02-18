(ns structural-typing.use.f-path-mismatches
  "Cases where no additions to the value could make it match the path"
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))


(fact "whole value is nil: impossible"
  (type! :String {[] string?})
  (check-for-explanations :String nil) => (just ["Value is nil, and that makes Sir Tony Hoare sad"]))

(fact "keywords"
  (type! :X {[:a :b] [required-path even?]})
  (fact "matching non-maps: impossible"
    (check-for-explanations :X 1) =>      (just (err:notpath [:a :b] 1))
    (check-for-explanations :X [:a 1]) => (just (err:notpath [:a :b] [:a 1]))
    (check-for-explanations :X {:a 1}) => (just (err:notpath [:a :b] {:a 1})))

  (fact "missing or nil values: truncated"
    (check-for-explanations :X {}) => (just (err:required [:a :b]))
    (check-for-explanations :X {:b 1}) => (just (err:required [:a :b]))
    (check-for-explanations :X {:a nil}) => (just (err:required [:a :b]))
    (check-for-explanations :X {:a {:c 1}}) => (just (err:required [:a :b]))))

(fact "indices"
  (type! :X {[1 0] [required-path even?]})
  (fact "non-collections: impossible"
    (check-for-explanations :X 1) =>      (just (err:notpath [1 0] 1))
    (check-for-explanations :X [[] 1]) => (just (err:notpath [1 0] [[] 1])))

  (fact "it is 'impossible' to apply an index to maps or sets"
    (check-for-explanations :X [[] {:a 1}]) => (just (err:notpath [1 0] [[] {:a 1}]))
    (check-for-explanations :X [[] #{0 1}]) => (just (err:notpath [1 0] [[] #{0 1}])))

  (fact "missing or nil values: truncated"
    (check-for-explanations :X [     ]) => (just (err:required [1 0]))
    (check-for-explanations :X [[]   ]) => (just (err:required [1 0]))
    (check-for-explanations :X [[] []]) => (just (err:required [1 0])))

  (fact "note that you can take the index of an infinite sequence"
    (let [result (built-like :X (repeat (list 2 1)))]
      (sequential? result) => true
      (take 3 result) => (take 3 (repeat (list 2 1))))))

(fact "ALL"
  (type! :X {[ALL ALL] [required-path even?]})
  (fact "non-collections: impossible"
    (check-for-explanations :X 1) =future=>   (just (err:bad-all-target [ALL ALL] 1 1))
    (check-for-explanations :X [1]) =future=> (just (err:bad-all-target [ALL ALL] [1] 1)))
  (future-fact "nested missing or nil values: truncated"
    (check-for-explanations :X [     ]) => (just (err:required [0 ALL])))

  (type! :X {[:x ALL] [required-path even?]})
  (fact "as before, non-collections: impossible"
    (check-for-explanations :X {:x 1}) =future=>   (just (err:bad-all-target [:x ALL] {:x 1} 1)))
  (future-fact "A previously non-indexed element is checked for existence "
    (check-for-explanations :X {}) =>         (just (err:required :x)
                                                    (err:nil-all [:x ALL] {}))
    (check-for-explanations :X {:x nil}) =>   (just (err:required :x)
                                                    (err:nil-all [:x ALL] {:x nil})))
    
  (type! :X {[ALL :x] [required-path even?]})
  (fact "ALL passes a nil along to later elements"
    (check-for-explanations :X [{:x 1} {} {:x nil}]) => (just (err:shouldbe [0 :x] "even?" 1)
                                                              (err:required [1 :x])
                                                              (err:required [2 :x]))))

    
(fact "RANGE"
  (fact "non-collections: impossible"
    (let [path [(RANGE 1 2) (RANGE 1 2)]]
      (type! :X {path [required-path]})
      (check-for-explanations :X 8) =future=>   (just (err:bad-range-target path 8 8))
      (check-for-explanations :X [0 1]) =future=> (just (err:bad-range-target path [0 1] 1))))
  
  (fact "missing or nil values: truncated"
    (let [path [(RANGE 1 4) (RANGE 1 3)]]
      (type! :X {path [required-path]})
      (let [in [:unchecked [10 11 12] [20 21 22] [30 31 32] :unchecked]]
        (built-like :X in) => in)

      (let [in [ :unchecked [10 11 12] [20 21] [30] :unchecked]]
        (check-for-explanations :X in) => (just (err:required [2 2])
                                                (err:required [3 1])
                                                (err:required [3 2])))

      (let [in [ :unchecked [10 11 12] nil [30 31 32] :unchecked]]
        (check-for-explanations :X in) =future=> (just (err:required [2 1])
                                                (err:required [2 2])))

      (fact "in a combination of non-sequential value and truncation, you only see impossible path"
        (let [in [ :unchecked [10 11 12] :oops [30 31 32] :unchecked]]
          (check-for-explanations :X in) =future=> (just (err:bad-range-target path in :oops))))))

  (fact "completely empty arrays count as truncation"
    (let [path [(RANGE 1 2) (RANGE 1 3)]]
      (type! :X {path [required-path]})
      (fact "top level"
        (check-for-explanations :X []) =future=> (just (err:required [1 1])
                                                (err:required [1 2])))
      (fact "nested"
        (check-for-explanations :X [ [] ]) =future=> (just (err:required [1 1])
                                                    (err:required [1 2])))))

  (fact "cannot take maps or sets"
    (type! :X (requires [(RANGE 1 2)]))
    (let [bad {:a 1, :b 2, :c 3}]
      (check-for-explanations :X bad) =future=> (just (err:bad-range-target [(RANGE 1 2)] bad bad)))

    (let [bad #{1 2 3 4 5}]
      (check-for-explanations :X bad) =future=> (just (err:bad-range-target [(RANGE 1 2)] bad bad)))))

(fact "a path predicate that blows up counts as an impossible path"
  (type! :X {[:a pos?] even?})
  (check-for-explanations :X {:a "string"}) => (just (err:notpath '[:a pos?] {:a "string"})))


(fact "Some random leftover tests"
  (type! :Points {[ALL :x] integer?
                  [ALL :y] integer?})
  (check-for-explanations :Points 3) =future=> (just (err:bad-all-target [ALL :x] 3 3)
                                              (err:bad-all-target [ALL :y] 3 3))
  
  (check-for-explanations :Points [1 2 3]) => (just (err:notpath [ALL :x] [1 2 3])
                                                    (err:notpath [ALL :y] [1 2 3]))
  
  (fact "works for partial collections"
    (type! :Figure (requires :color [:points ALL (each-of :x :y)]))
    (check-for-explanations :Figure {:points 3})
    =future=> (just (err:required :color)
             (err:bad-all-target [:points ALL :x] {:points 3} 3)
             (err:bad-all-target [:points ALL :y] {:points 3} 3)))

  (fact "ALL following ALL"
    (type! :Nesty {[:x ALL ALL :y] integer?})
    (check-for-explanations :Nesty {:x [1]}) (just (err:notpath [:x ALL ALL :y] {:x [1]}))))
