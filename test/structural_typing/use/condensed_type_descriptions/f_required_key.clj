(ns structural-typing.use.condensed-type-descriptions.f-required-key
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)


(fact "use of ALL"

  (fact "final ALL forces a preceding key to be required"
    (type! :Terminal {[:a ALL] [required-key even?]})
    (check-for-explanations :Terminal {}) => [(err:required :a)])

  (fact "A middle ALL requires the preceding and following key"
    (type! :Middle {[:a ALL :b] [even? required-key]}) ; doesn't matter where `required-key` is.
    (check-for-explanations :Middle {}) => [(err:required :a)]
    (check-for-explanations :Middle {:a [{:c 1}]}) => [(err:required [:a 0 :b])]

    (fact "However, it *does* allow an empty collection"
      (checked :Middle {:a []}) => {:a []}))

  (fact "there may be more than one ALL in the path"
    (type! :Double {[:a ALL :b ALL] [required-key even?]})
    (check-for-explanations :Double {}) => [(err:required :a)]
    (checked :Double {:a []}) => {:a []}
    (check-for-explanations :Double {:a [{:c 1}]}) => [(err:required [:a 0 :b])]
    (checked :Double {:a [{:b []}]}) => {:a [{:b []}]}
    (check-for-explanations :Double {:a [{:b [1]}]}) => [(err:shouldbe [:a 0 :b 0] "even?" 1)]
    (checked :Double {:a [{:b [2 4]}]}) => {:a [{:b [2 4]}]})

  (fact "ALL may be present in a shorthand `requires`"
    (type! :X (requires [:a ALL :b]))
    (check-for-explanations :Middle {}) => [(err:required :a)]
    (check-for-explanations :Middle {:a [{:c 1}]}) => [(err:required [:a 0 :b])]
    (checked :Middle {:a []}) => {:a []}))

    


(start-over!)
