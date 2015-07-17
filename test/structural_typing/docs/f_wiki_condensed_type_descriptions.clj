(ns structural-typing.docs.f-wiki-condensed-type-descriptions
 (:require [structural-typing.type :refer :all]
           [structural-typing.global-type :refer :all]
           [clojure.string :as str])
 (:use midje.sweet))

(start-over!)

(type! :V1
       {[:x] [required-key]}
       {[:x] [even?]})
(type! :V2
       {[:x] [required-key even?]})

(tabular
  (fact 
    (with-out-str (checked ?version {}))
    => #":x must exist"
    
    (with-out-str (checked ?version {:x 1}))
    => #":x should be `even\?`")
  ?version
  :V1
  :V2)

(type! :V1 {[:x] [even?]})
(type! :V2 { :x   even? })

(tabular
  (fact 
    (checked ?version {:x 2}) => {:x 2}
    
    (with-out-str (checked ?version {:x 1}))
    => #":x should be `even\?`")
  ?version
  :V1
  :V2)

(type! :V1 {[:refpoint :x] [integer?]
            [:refpoint :y] [integer?]})
(type! :V2 {:refpoint {:x integer? 
            :y integer?}})

(tabular
  (fact 
    (with-out-str (checked ?version {:refpoint {:y "2"}}))
    => #"\[:refpoint :y\] should be `integer\?`")
  ?version
  :V1
  :V2)


(type! :Point {:x integer? :y integer?})
(type! :X {:refpoint (includes :Point)}
          {:refpoint {:color integer?}})

(fact 
  (with-out-str (checked :X {:refpoint {:y "2"}}))
  => #"\[:refpoint :y\] should be `integer\?`")


(type! :X {[:refpoint [:x :y]] integer?})
(fact 
  (with-out-str (checked :X {:refpoint {:y "2"}}))
  => #"\[:refpoint :y\] should be `integer\?`")

(type! :X {[:refpoint (forks :x :y)] integer?})
(fact 
  (with-out-str (checked :X {:refpoint {:y "2"}}))
  => #"\[:refpoint :y\] should be `integer\?`")


(type! :Point [:x :y] {:x integer? :y integer?, :color string?})
(fact 
  (with-out-str (checked :Point {:y 1}))
  => #":x must exist"
  (with-out-str (checked :Point {:x "1" :y 1}))
  => #":x should be `integer\?`"
  (with-out-str (checked :Point {:x 1 :y 1, :color 1}))
  => #":color should be `string\?`"
  )

(type! :A-has-evens {[:a ALL] even?})
(fact 
  (with-out-str (checked :A-has-evens {:a [1 2]}))
  => #"\[:a 0\] should be `even")

(type! :Terminal {[:a ALL] [required-key even?]})
(type! :Middle {[:a ALL :b] [required-key even?]})
(type! :Double {[:a ALL :b ALL] [required-key even?]})
(fact 
  (with-out-str (checked :Terminal {})) => #":a must exist"

  (with-out-str (checked :Middle {})) => #":a must exist"
  (checked :Middle {:a []}) => {:a []}
  (with-out-str (checked :Middle {:a [{:c 1}]})) => #"\[:a 0 :b\] must exist"

  (with-out-str (checked :Double {})) => #":a must exist"
  (checked :Double {:a []}) => {:a []}
  (with-out-str (checked :Double {:a [{:c 1}]})) => #"\[:a 0 :b\] must exist"
  (checked :Double {:a [{:b []}]}) => {:a [{:b []}]}
  (with-out-str (checked :Double {:a [{:b [1]}]})) => #"\[:a 0 :b 0\] should be `even"
  (checked :Double {:a [{:b [2 4]}]}) => {:a [{:b [2 4]}]})


(type! :DoubleNested {[:a ALL :b ALL] even?})
(fact 
  (with-out-str (checked :DoubleNested {:a [{:b [4 8]} {:b [0 2]} {:b [1 2 4]}]}))
  => #"\[:a 2 :b 0\] should be `even\?")


(type! :Figure {[:points ALL [:x :y]] [required-key integer?]})
(fact 
  (let [result (with-out-str (checked :Figure {:points [{:x "1"}]}))]
    result => #"\[:points 0 :y\] must exist"
    result => #"\[:points 0 :x\] should be `integer"))


(type! :Point {:x integer? :y integer?})
(type! :V1 {[:points ALL] (includes :Point)})
(type! :V2 {[:points ALL] {:x integer? :y integer?}})

(tabular
  (fact 
    (checked ?version {:x 2}) => {:x 2}
    
    (with-out-str (checked ?version {:points [{:x "1" :y 1}]}))
    => #"\[:points 0 :x\] should be `integer")
  ?version
  :V1
  :V2)

(type! :V1 [:x [:y :z]])

(type! :V2 {[:x] [required-key]
            [:y :z] [required-key]})
(type! :V3 (requires :x [:y :z]))

(tabular
  (fact 
    (checked ?version {:x 2
                       :y {:z 1}}) => {:x 2
                                       :y {:z 1}}
    
    (with-out-str (checked ?version {:x 2 :y 3}))
    => #"\[:y :z\] must exist")
  ?version
  :V1
  :V2
  :V3)

(type! :Point
       [requires :x :y]
       {:x integer? :y integer?})

(type! :Line1
       [:start :end :color]
       {:color string?
        :start (includes :Point)
        :end (includes :Point)})

(type! :Line2
       (requires [:start (includes :Point)]
                 [:end (includes :Point)]
                 :color)
       {:color string?})

(tabular
  (fact 
    (let [result (with-out-str (checked ?version {:start {:x 1 :y "2"}}))]
      result => #":color must exist"
      result => #"\[:end :x\] must exist"
      result => #"\[:end :y\] must exist"
      result => #"\[:start :y\] should be `integer"))
  ?version
  :Line1
  :Line2)


(type! :Point1
       (requires :x :y :color)
       {[(forks :x :y)] integer?})

(type! :Point2
       {[:x] [required-key integer?]
        [:y] [required-key integer?]
        [:color] [required-key]})

(tabular
  (fact 
    (let [result (with-out-str (checked ?version {:x 3.0}))]
      result => #":color must exist"
      result => #":x should be `integer"
      result => #":y must exist"))
  ?version
  :Point1
  :Point2)




;;; Took out the documentation for this feature, but it still exists.
(future-fact "Delete undocumented feature?")

(type! :Figure1
       (requires :color
                 [:points ALL (includes :Point)])
       {:color string?})

(type! :Figure2
       (requires :color :points)
       {:color string?
        [:points ALL] (includes :Point)})


(tabular 
  (fact
    (let [result (with-out-str (checked ?version {:points [{:x 1 :y "2"}]}))]
      result => #":color must exist"
      result => #"\[:points 0 :y\] should be `integer"))
  ?version
  :Figure1
  :Figure2)



(start-over!)
