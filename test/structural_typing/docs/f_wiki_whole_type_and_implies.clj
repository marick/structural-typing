(ns structural-typing.docs.f-wiki-whole-type-and-implies
 (:require [structural-typing.type :refer :all]
           [structural-typing.global-type :refer :all]
           [structural-typing.preds :as pred])
 (:use midje.sweet structural-typing.pred-writing.testutil))

(start-over!)

(type! :String string?)

(fact
  (checked :String "foo") => "foo"
  (check-for-explanations :String nil) => (just ["Value is nil, and that makes Sir Tony Hoare sad"])
  (check-for-explanations :String 1) => (just ["Value should be `string?`; it is `1`"]))

(type! :String {[] string?})
(fact "above is shorthand"
  (checked :String "foo") => "foo"
  (check-for-explanations :String nil) => (just ["Value is nil, and that makes Sir Tony Hoare sad"])
  (check-for-explanations :String 1) => (just ["Value should be `string?`; it is `1`"]))




(type! :Odd
       map?
       (show-as "even size" (comp even? count))
       {:b [required-key string?]})

(fact
  (checked :Odd {:a 1, :b "foo"}) => {:a 1, :b "foo"}
  (check-for-explanations :Odd [1 2]) => (just ":b must exist and be non-nil"
                                               "Value should be `map?`; it is `[1 2]`")
  (check-for-explanations :Odd {:b 1}) => (just ":b should be `string?`; it is `1`"
                                                "Value should be `even size`; it is `{:b 1}`"))




(type! :X (pred/implies (comp odd? :a) {:b [pos? even?]}))

(fact
  (checked :X {:a 2, :b -1}) => {:a 2, :b -1}
  (checked :X {:b -1}) => {:b -1}
  (check-for-explanations :X {:a 1, :b -1}) => (just ":b should be `even?`; it is `-1`"
                                                     ":b should be `pos?`; it is `-1`"))

(type! :X (pred/implies :a {:b odd?}))
(fact
  (checked :X {:a 2, :b 1}) => {:a 2, :b 1}
  (check-for-explanations :X {:a 2, :b 2}) => (just ":b should be `odd?`; it is `2`")
  (checked :X {:b 2}) => {:b 2})

(type! :X (pred/implies :a (requires :b :c :d)))
(fact 
  (checked :X {}) => {}
  (check-for-explanations :X {:a 1}) => (contains #":b must exist"))

(type! :Point (requires :x :y))
(type! :S (pred/implies (comp true? :sidecar?) {:the-sidecar (includes :Point)}))

;;;; BUG
(type! :Point [:x :y])
(type! :Includer {:the-sidecar (includes :Point)})
(type! :Direct {[:the-sidecar :x] required-key
                [:the-sidecar :y] required-key})


(type! :I-Includer (pred/implies (comp true? :sidecar?) {:the-sidecar (includes :Point)}))
(type! :I-Direct (pred/implies (comp true? :sidecar?) {[:the-sidecar :x] required-key
                                                       [:the-sidecar :y] required-key}))
(fact "bug"
  (fact "the non-implied types work"
    (check-for-explanations :Point {}) => (just #":x must exist" #":y must exist")

    (check-for-explanations :Includer {}) => (just #":the-sidecar :x] must exist" #":the-sidecar :y] must exist")
    (check-for-explanations :Includer {:the-sidecar 3}) => (just #":the-sidecar :x] must" #":the-sidecar :y] must")
    (check-for-explanations :Includer {:the-sidecar {}}) => (just #":the-sidecar :x] must" #":the-sidecar :y] must")
    (check-for-explanations :Includer {:the-sidecar {:x 1}}) => (just #":the-sidecar :y] must exist")

    (check-for-explanations :Direct {}) => (just #":the-sidecar :x" #":the-sidecar :y")
    (check-for-explanations :Direct {}) => (just #":the-sidecar :x] must exist" #":the-sidecar :y] must exist")
    (check-for-explanations :Direct {:the-sidecar 3}) => (just #":the-sidecar :x] must" #":the-sidecar :y] must")
    (check-for-explanations :Direct {:the-sidecar {}}) => (just #":the-sidecar :x] must" #":the-sidecar :y] must")
    (check-for-explanations :Direct {:the-sidecar {:x 1}}) => (just #":the-sidecar :y] must exist"))



  (fact "the I-Direct case works"
    (checked :I-Direct {}) => {}
    (checked :I-Direct {:sidecar? false}) => {:sidecar? false}
    (checked :I-Direct {:sidecar? true :the-sidecar {:x 1 :y 2}})
    =>                 {:sidecar? true :the-sidecar {:x 1 :y 2}}

    (check-for-explanations :I-Direct {:sidecar? true}) => (just #":the-sidecar :x] must" #":the-sidecar :y] must")
    (check-for-explanations :I-Direct {:sidecar? true :the-sidecar 3}) => (just #":the-sidecar :x] must" #":the-sidecar :y] must")
    (check-for-explanations :I-Direct {:sidecar? true :the-sidecar {}}) => (just #":the-sidecar :x] must" #":the-sidecar :y] must")
    (check-for-explanations :I-Direct {:sidecar? true :the-sidecar {:x 1}}) => (just #":the-sidecar :y] must")
    )

  ;; There is, for the time being, a losing error message
  (check-for-explanations :I-Includer {}) => (just #"sorry I can't give a better error message")
  (future-fact "the I-Includer case works"
    (checked :I-Includer {}) => {}
    (checked :I-Includer {:sidecar? false}) =future=> {:sidecar? false}
    (checked :I-Includer {:sidecar? true :the-sidecar {:x 1 :y 2}})
    =future=>                   {:sidecar? true :the-sidecar {:x 1 :y 2}}

    (check-for-explanations :I-Includer {:sidecar? true}) =future=> (just #":the-sidecar :x] must" #":the-sidecar :y] must")
    (check-for-explanations :I-Includer {:sidecar? true :the-sidecar 3}) =future=> (just #":the-sidecar :x] must" #":the-sidecar :y] must")
    (check-for-explanations :I-Includer {:sidecar? true :the-sidecar {}}) =future=> (just #":the-sidecar :x] must" #":the-sidecar :y] must")
    (check-for-explanations :I-Includer {:sidecar? true :the-sidecar {:x 1}}) =future=> (just #":the-sidecar :y] must")
    )
  )

  


(start-over!)
