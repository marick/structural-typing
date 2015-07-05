(ns structural-typing.f-frob
  (:require [structural-typing.frob :as subject])
  (:use midje.sweet))

(fact update-each-value
  (subject/update-each-value {} inc) => {}
  (subject/update-each-value {:a 1, :b 2} inc) => {:a 2 :b 3}
  (subject/update-each-value {:a [], :b [:b]} conj 1) => {:a [1] :b [:b 1]})

(fact wrap-pred-with-catcher
  (let [wrapped (subject/wrap-pred-with-catcher even?)]
    (wrapped 2) => true
    (wrapped 3) => false
    (even? nil) => (throws)
    (wrapped nil) => false))
  
(fact force-vector
  (subject/force-vector 1) => (vector 1)
  (subject/force-vector [1]) => (vector 1)
  (subject/force-vector '(1)) => (vector 1))

(fact "making a map with uniform keys"
  (subject/mkmap:all-keys-with-value [] 3) => {}
  (subject/mkmap:all-keys-with-value [:a] 3) => {:a 3}
  (subject/mkmap:all-keys-with-value [:a [:b]] 3) => {:a 3, [:b] 3})


(fact "`mkst:x->abc` converts (possibly optionally) each element of a lazyseq and replaces it with N results"
  (fact "one arg form processes each element"
    ( (subject/mkst:x->abc #(repeat % %)) [1 2 3]) => [1 2 2 3 3 3])

  (fact "two arg form processes only elements that match predicate"
    ( (subject/mkst:x->abc #(repeat % %) even?) [1 2 3 4]) => [1 2 2 3 4 4 4 4])

  (fact "empty sequences are handled"
    ( (subject/mkst:x->abc #(repeat % %) even?) []) => empty?)

  (fact "it is indeed lazy"
    (let [made (subject/mkst:x->abc #(repeat % %) even?)]
      (take 2 (made [0])) => empty?
      (take 2 (made [0 1])) => [1]
      (take 2 (made [0 1 2])) => [1 2]
      (take 2 (made [0 1 2 3])) => [1 2]
      (take 2 (made [0 1 2 3 4])) => [1 2]
      (take 2 (made (range))) => [ 1 2 ]
      (count (take 100000 (made (range)))) => 100000)))


(fact "`mkst:x->xabc` converts (possibly optionally) each element of a lazyseq and replaces it
       with N results. The first argument is preserved"
  (fact "one arg form processes each element"
    ( (subject/mkst:x->xabc #(repeat % (- %))) [1 2 3]) => [1 -1 2 -2 -2 3 -3 -3 -3])

  (fact "two arg form processes only elements that match predicate"
    ( (subject/mkst:x->xabc #(repeat % (- %)) even?) [1 2 3 4]) => [1 2 -2 -2 3 4 -4 -4 -4 -4]))


(fact "`mkst:x->y` converts (possibly optionally) each element of a lazyseq and replaces it
       with 1 result."
  (fact "one arg form processes each element"
    ( (subject/mkst:x->y -) [1 2 3]) => [-1 -2 -3 ])

  (fact "two arg form processes only elements that match predicate"
    ( (subject/mkst:x->y - even?) [1 2 3 4]) => [1 -2 3 -4]))
