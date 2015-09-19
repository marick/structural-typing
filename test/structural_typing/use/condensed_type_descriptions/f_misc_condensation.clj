(ns structural-typing.use.condensed-type-descriptions.f-misc-condensation
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)

(fact "single elements need not be in collections"
  (type! :V1 {[:x] [even?]})
  (type! :V2 { :x   even? })

  (tabular
    (fact 
      (checked ?version {:x 2}) => {:x 2}
      (check-for-explanations ?version {:x 1}) => [(err:shouldbe :x "even?" 1)])
    ?version
    :V1
    :V2))

(fact "a map can be used in predicates"
  (type! :V1 {[:refpoint :x] [integer?]
              [:refpoint :y] [integer?]})
  (type! :V2 {:refpoint {:x integer? 
                         :y integer?}})

  (tabular
    (fact 
      (check-for-explanations ?version {:refpoint {:y "2"}})
      => [(err:shouldbe [:refpoint :y] "integer?" "\"2\"")])
    ?version
    :V1
    :V2))





(start-over!)

