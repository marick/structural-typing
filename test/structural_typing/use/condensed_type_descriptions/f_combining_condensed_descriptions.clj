(ns structural-typing.use.condensed-type-descriptions.f-combining-condensed-descriptions
  (:require [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(start-over!)

(fact "predicates are combined"
  (type! :V1
         {[:x] [required-key]}
         {[:x] [even?]})
  (type! :V2
         {[:x] [required-key even?]})

  (tabular
    (fact 
      (check-for-explanations ?version {}) => [(err:required :x)]
      (check-for-explanations ?version {:x 1}) => [(err:shouldbe :x "even?" 1)])
    ?version
    :V1
    :V2))

(fact "combining `requires` and a map"
  (type! :Point (requires :x :y) {:x integer? :y integer?, :color string?})

  (check-for-explanations :Point {:y 1}) => [(err:required :x)]
  (check-for-explanations :Point {:x "1" :y 1}) => [(err:shouldbe :x "integer?" "\"1\"")]
  (check-for-explanations :Point {:x 1 :y 1, :color 1}) => [(err:shouldbe :color "string?" 1)])

(fact "combining `requires` with a forking map"
  (type! :Point1
         (requires :x :y :color)
         {[(through-each :x :y)] integer?})
  
  (type! :Point2
         {[:x] [required-key integer?]
          [:y] [required-key integer?]
          [:color] [required-key]})
  
  (tabular
    (fact
      (check-for-explanations ?version {:x 3.0})
      => [(err:required :color)
          (err:shouldbe :x "integer?" 3.0)
          (err:required :y)])
    ?version
    :Point1
    :Point2))

(fact "combining requires"
  (type! :X (requires :a :b :c [:d :e]))
  (type! :Y (requires :a) (requires :b) (requires :c) (requires [:d :e]))

  (description :X) => '{[:a] [required-key], [:b] [required-key], [:c] [required-key], [:d :e] [required-key]}
  (description :Y) => (description :X))

(fact "combining descriptions containing ALL"
  (type! :X
         (requires [:a ALL :c] [:b :f ALL])
         {:a even?}
         {[:b :f ALL] even?})
  (type! :Y {[:a ALL :c] [required-key]
             [:b :f ALL] [required-key even?]
             [:a] [even?]})
  (description :Y) => (description :X))

(fact "duplicates are ignored"
  (type! :X {:a required-key :b even?} (requires :b :a) {:b even?})
  (type! :Y {[:a] [required-key]
             [:b] [required-key even?]})
  (description :Y) => (description :X))


(fact "a realistic example"
  
  (type! :Point
         (requires :x :y)
         {:x integer? :y integer?})

  (type! :Line1
         (requires :start :end :color)
         {:color string?
          :start (includes :Point)
          :end (includes :Point)})
  
  (type! :Line2
         (requires [:start (paths-of :Point)]
                   [:end (paths-of :Point)]
                   :color)
         {:color string?
          :start (includes :Point)
          :end (includes :Point)})
  
  (tabular
    (fact 
      (let [result (check-for-explanations ?version {:start {:x 1 :y "2"}})]
        result => (contains (err:required :color)
                            (err:required [:end :x])
                            (err:required [:end :y])
                            (err:shouldbe [:start :y] "integer?" "\"2\"")
                            :in-any-order :gaps-ok)
        ))
    ?version
    :Line1
    :Line2))

(fact "other variations on combining types"
  (type! :Point
         (requires :x :y)
         {:x integer? :y integer?})
  
  (type! :ColorfulPoint
         {:x [required-key integer?]
          :y [required-key integer?]
          :color [required-key string?]})
  
  (fact
    (check-for-explanations :ColorfulPoint {:y 1 :color 1})
    => (just (err:shouldbe :color "string?" 1)
             (err:required :x)))
  
  (type! :ColorfulPoint
         (includes :Point)
         {:color string?})
  (fact
    (check-for-explanations :ColorfulPoint {:y 1 :color 1})
    => (just (err:shouldbe :color "string?" 1)
             (err:required :x)))
  
  (type! :Colorful {:color [required-key string?]})
  (type! :ColorfulPoint (includes :Point) (includes :Colorful))
  
  (fact
    (check-for-explanations :ColorfulPoint {:y 1 :color 1})
    => (just (err:shouldbe :color "string?" 1)
             (err:required :x)))
  
  (fact "combining types at check time"
    (check-for-explanations [:Colorful :Point] {:y 1 :color 1})
    => (just (err:shouldbe :color "string?" 1)
             (err:required :x))))
  



(start-over!)
