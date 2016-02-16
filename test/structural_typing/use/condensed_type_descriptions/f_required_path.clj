(ns structural-typing.use.condensed-type-descriptions.f-required-path
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)

(fact "`required-path` combines with other predicates"
  (type! :Point {:x [required-path integer?]
                 :y [required-path integer?]})
  
  (check-for-explanations :Point {:x "1"})
  => (just (err:shouldbe :x "integer?" "\"1\"")
           (err:required :y)))

(fact "The `requires` function is shorthand for required-path"
  (type! :Point
       (requires :x :y)
       {:x integer? :y integer?})

  (check-for-explanations :Point {:x "1"})
  => (just (err:shouldbe :x "integer?" "\"1\"")
           (err:required :y)))


(fact "use of ALL"

  (fact "final ALL forces a preceding key to be required"
    (type! :Terminal {[:a ALL] [required-path even?]})
    (check-for-explanations :Terminal {}) =future=> [(err:required :a)
                                              (err:notpath [:a ALL] {})]

    ;; or would this be better?
    (check-for-explanations :Terminal {}) =future=> [(err:required :a)
                                                     (err:nil-all [:a ALL] {})])

  (fact "A middle ALL requires the preceding and following key"
    (type! :Middle {[:a ALL :b] [even? required-path]}) ; doesn't matter where `required-path` is.
    (check-for-explanations :Middle {}) =future=> [(err:required :a)
                                            (err:notpath [:a ALL :b] {})]

    ;; or would this be better?
    (check-for-explanations :Middle {}) =future=> [(err:required :a)
                                                   (err:nil-all [:a ALL :b] {})]
    (check-for-explanations :Middle {:a [{:c 1}]}) => [(err:required [:a 0 :b])]

    (fact "However, it *does* allow an empty collection"
      (built-like :Middle {:a []}) => {:a []}))

  (fact "there may be more than one ALL in the path"
    (type! :Double {[:a ALL :b ALL] [required-path even?]})
    (check-for-explanations :Double {}) =future=> [(err:required :a)
                                            (err:notpath [:a ALL :b ALL] {})
                                            (err:notpath [:a ALL :b] {})]

    ;; or would this be better?
    (check-for-explanations :Double {}) =future=> [(err:required :a)
                                            (err:nil-all [:a ALL :b ALL] {})
                                            ;; This next is spurious, but tolerable
                                            (err:nil-all [:a ALL :b] {})]
    (built-like :Double {:a []}) => {:a []}
    (check-for-explanations :Double {:a [{:c 1}]}) =future=> [(err:required [:a 0 :b])
                                                       (err:notpath [:a ALL :b ALL] {:a [{:c 1}]})]
    ;; or would this be better?
    (check-for-explanations :Double {:a [{:c 1}]}) =future=> [(err:required [:a 0 :b])
                                                              (err:nil-all [:a ALL :b ALL] {:a [{:c 1}]})]
    (built-like :Double {:a [{:b []}]}) => {:a [{:b []}]}
    (check-for-explanations :Double {:a [{:b [1]}]}) => [(err:shouldbe [:a 0 :b 0] "even?" 1)]
    (built-like :Double {:a [{:b [2 4]}]}) => {:a [{:b [2 4]}]})

  (fact "ALL may be present in a shorthand `requires`"
    (type! :X (requires [:a ALL :b]))
    (check-for-explanations :Middle {}) =future=> [(err:required :a) (err:notpath [:a ALL :b] {})]
    ;; or would this be better?
    (check-for-explanations :Middle {}) =future=> [(err:required :a) (err:nil-all [:a ALL :b] {})]
    (check-for-explanations :Middle {:a [{:c 1}]}) => [(err:required [:a 0 :b])]
    (built-like :Middle {:a []}) => {:a []}))

(start-over!)
