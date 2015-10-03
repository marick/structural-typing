(ns structural-typing.use.condensed-type-descriptions.f-includes
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)

(fact "You can use `includes` instead of an explicit map"
  (type! :Point {:x integer? :y integer?})
  (type! :X {:refpoint (includes :Point)}
            {:refpoint {:color integer?}})

  (check-for-explanations :X {:refpoint {:y "2"}})
  => [(err:shouldbe [:refpoint :y] "integer?" "\"2\"")])

(fact "`includes` can be used as an entire right-hand-side"
  (type! :Point {:x integer? :y integer?})
  (type! :V1 {[:points ALL] (includes :Point)})
  (type! :V2 {[:points ALL] {:x integer? :y integer?}})

  (tabular
    (fact 
      (built-like ?version {:x 2}) => {:x 2}
      (check-for-explanations ?version {:points [{:x "1" :y 1}]})
      => [(err:shouldbe [:points 0 :x] "integer?" "\"1\"")])
    ?version
    :V1
    :V2))

(fact "`include` can also be used to be part of a predicate list"
  (type! :A {:a even?})
  (type! :X {[:as] [required-key (includes :A)]})
  (type! :Y {[:as] required-key
             [:as :a] even?})
  (description :X) => (description :Y))
  




;;; Implies

(type! :Point (requires :x :y))
(type! :Includer {:the-sidecar (includes :Point)})
(type! :Direct {[:the-sidecar :x] required-key
                [:the-sidecar :y] required-key})


(fact "for reference: the basic types work"
  (check-for-explanations :Point {}) => (just (err:required :x) (err:required :y))
  
  (check-for-explanations :Includer {}) => (just (err:required [:the-sidecar :x])
                                                 (err:required [:the-sidecar :y]))
  (check-for-explanations :Includer {:the-sidecar 3})
  => (just (err:notpath [:the-sidecar :x] {:the-sidecar 3})
           (err:notpath [:the-sidecar :y] {:the-sidecar 3}))
  (check-for-explanations :Includer {:the-sidecar {}}) => (just (err:required [:the-sidecar :x])
                                                                (err:required [:the-sidecar :y]))
  (check-for-explanations :Includer {:the-sidecar {:x 1}})
  => (just (err:required [:the-sidecar :y]))
  
  (check-for-explanations :Direct {}) => (just (err:required [:the-sidecar :x])
                                               (err:required [:the-sidecar :y]))
  (check-for-explanations :Direct {}) => (just (err:required [:the-sidecar :x])
                                               (err:required [:the-sidecar :y]))
  (check-for-explanations :Direct {:the-sidecar 3})
  => (just (err:notpath [:the-sidecar :x] {:the-sidecar 3})
           (err:notpath [:the-sidecar :y] {:the-sidecar 3}))
  (check-for-explanations :Direct {:the-sidecar {}}) => (just (err:required [:the-sidecar :x])
                                                              (err:required [:the-sidecar :y]))
  (check-for-explanations :Direct {:the-sidecar {:x 1}}) => (just (err:required [:the-sidecar :y])))


(fact "for reference: implies without `includes` works"
  (type! :I-Direct (pred/implies (comp true? :sidecar?) {[:the-sidecar :x] required-key
                                                         [:the-sidecar :y] required-key}))
  (built-like :I-Direct {}) => {}
  (built-like :I-Direct {:sidecar? false}) => {:sidecar? false}
  (built-like :I-Direct {:sidecar? true :the-sidecar {:x 1 :y 2}})
  =>                    {:sidecar? true :the-sidecar {:x 1 :y 2}}
  
  (check-for-explanations :I-Direct {:sidecar? true})
  => (just (err:required [:the-sidecar :x]) (err:required [:the-sidecar :y]))
  (check-for-explanations :I-Direct {:sidecar? true :the-sidecar 3})
  => (just (err:notpath [:the-sidecar :x] {:sidecar? true :the-sidecar 3})
           (err:notpath [:the-sidecar :y] {:sidecar? true :the-sidecar 3}))
  (check-for-explanations :I-Direct {:sidecar? true :the-sidecar {}})
  => (just (err:required [:the-sidecar :x]) (err:required [:the-sidecar :y]))
  (check-for-explanations :I-Direct {:sidecar? true :the-sidecar {:x 1}})
  => (just (err:required [:the-sidecar :y])))

(fact "You can use `includes` in the antecedent part of an `implies`"
  (type! :I-Includer (pred/implies (comp true? :sidecar?) {:the-sidecar (includes :Point)}))
  (built-like :I-Includer {}) => {}
  (built-like :I-Includer {:sidecar? false}) => {:sidecar? false}
  (built-like :I-Includer {:sidecar? true :the-sidecar {:x 1 :y 2}})
  =>                      {:sidecar? true :the-sidecar {:x 1 :y 2}}

  (check-for-explanations :I-Includer {:sidecar? true})
  => (just (err:required [:the-sidecar :x])
           (err:required [:the-sidecar :y]))
  (check-for-explanations :I-Includer {:sidecar? true :the-sidecar 3})
  => (just (err:notpath [:the-sidecar :x] {:sidecar? true :the-sidecar 3})
           (err:notpath [:the-sidecar :y] {:sidecar? true :the-sidecar 3}))
  (check-for-explanations :I-Includer {:sidecar? true :the-sidecar {}})
  => (just (err:required [:the-sidecar :x])
           (err:required [:the-sidecar :y]))
  (check-for-explanations :I-Includer {:sidecar? true :the-sidecar {:x 1}})
  => (just (err:required [:the-sidecar :y])))

(fact "You can use `includes` in the antecedent part of an `implies`"
  (type! :I-Includer (pred/implies (comp true? :sidecar?) {:the-sidecar (includes :Point)}))
  (built-like :I-Includer {}) => {}
  (built-like :I-Includer {:sidecar? false}) => {:sidecar? false}
  (built-like :I-Includer {:sidecar? true :the-sidecar {:x 1 :y 2}})
  =>                   {:sidecar? true :the-sidecar {:x 1 :y 2}}

  (check-for-explanations :I-Includer {:sidecar? true})
  => (just (err:required [:the-sidecar :x])
           (err:required [:the-sidecar :y]))
  (check-for-explanations :I-Includer {:sidecar? true :the-sidecar 3})
  => (just (err:notpath [:the-sidecar :x] {:sidecar? true :the-sidecar 3})
           (err:notpath [:the-sidecar :y] {:sidecar? true :the-sidecar 3}))
  (check-for-explanations :I-Includer {:sidecar? true :the-sidecar {}})
  => (just (err:required [:the-sidecar :x])
           (err:required [:the-sidecar :y]))
  (check-for-explanations :I-Includer {:sidecar? true :the-sidecar {:x 1}})
  => (just (err:required [:the-sidecar :y])))

(fact "You can use `includes` in an `all-of`"
  (type! :OptionalX {:x even?})
  (type! :Implies (pred/implies :a (pred/all-of :x (includes :OptionalX))))
  (built-like :Implies {:a 1, :x 2}) => {:a 1, :x 2}
  (check-for-explanations :Implies {:a 1}) => [(err:required :x)]
  (check-for-explanations :Implies {:a 1, :x 1}) => [(err:shouldbe :x "even?" 1)]
  
  (fact "... even nested"
    (type! :OptionalX {:x even?})
    (type! :Implies (pred/implies :a (pred/all-of :b {:b (includes :OptionalX)})))
    (built-like :Implies {:a 1, :b {:x 2}}) => {:a 1, :b {:x 2}}
    (check-for-explanations :Implies {:a 1, :b {:x 1}}) => [(err:shouldbe [:b :x] "even?" 1)]))

(start-over!)
