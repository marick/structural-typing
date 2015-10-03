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
  (fact "matching non-maps: impossible"
    (type! :X {[:a :b] [required-key even?]})
    (check-for-explanations :X 1) =>      (just (err:notpath [:a :b] 1))
    (check-for-explanations :X [:a 1]) => (just (err:notpath [:a :b] [:a 1]))
    (check-for-explanations :X {:a 1}) => (just (err:notpath [:a :b] {:a 1})))

  (fact "missing or nil values: truncated"
    (check-for-explanations :X {}) => (just (err:required [:a :b]))
    (check-for-explanations :X {:b 1}) => (just (err:required [:a :b]))
    (check-for-explanations :X {:a nil}) => (just (err:required [:a :b]))
    (check-for-explanations :X {:a {:c 1}}) => (just (err:required [:a :b]))))

(fact "indices"
  (fact "non-collections: impossible"
    (type! :X {[1 0] [required-key even?]})
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
  









(future-fact "What happens when you use [:a ALL :c] on {}")

(future-fact "indexes in paths"
  (fact "one index"
    (type! :X {[:a 2] {:b even?}})
    (check-for-explanations :X {:a [0 {:b 1}]}) => (just (err:notpath [:a 2 :b] {:a [0 {:b 1}]})))

  (fact "two indexes" 
    (type! :X {[1 1] even?}) 
    (check-for-explanations :X []) =>               (just (err:notpath [1 1] []))
    (check-for-explanations :X [ [00 01] ]) =>      (just (err:notpath [1 1] [[0 1]]))
    (check-for-explanations :X [ [00 01] [10] ]) => (just (err:notpath [1 1] [[0 1] [10]])))

  (fact "against non-indexical whole value"
    (check-for-explanations :X 1) => (just (err:notpath [1 1] 1))))


(future-fact "When traversing paths reveals that location of ALL is not a collection"
  (type! :Points {[ALL :x] integer?
                  [ALL :y] integer?})
  (check-for-explanations :Points 3) => (just (err:notpath [ALL :x] 3)
                                              (err:notpath [ALL :y] 3))
  
  (fact "Failure is annoying side effect of there being no distinction between a present nil and a missing key"
    
    (check-for-explanations :Points [1 2 3]) => (just (err:notpath [ALL :x] 3)
                                                      (err:notpath [ALL :y] 3)))
  
  (fact "works for partial collections"
    (type! :Figure (requires :color [:points ALL (each-of :x :y)]))
    (check-for-explanations :Figure {:points 3})
    => (just (err:required :color)
             (err:notpath [:points ALL :x] {:points 3})
             (err:notpath [:points ALL :y] {:points 3})))

  (fact "ALL following ALL"
    (type! :Nesty {[:x ALL ALL :y] integer?})
    (check-for-explanations :Nesty {:x [1]}) (just (err:notpath [:x ALL ALL :y] {:x [1]}))))
