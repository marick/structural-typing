(ns structural-typing.docs.f-readme
 (:require [structural-typing.type :refer :all]
           [structural-typing.global-type :refer :all]
           [clojure.string :as str])
 (:use [midje.sweet :exclude [exactly]]))

(start-over!)

(type! :Point {:x integer? :y integer?})

(fact
  (let [result (with-out-str (checked :Point {:x "one" :y "two"}))]
    result => #":y should be `integer.*two"
    result => #":x should be `integer.*one"))


;;; The throwing error handler.

(on-error! throwing-error-handler)

(fact
  (checked :Point {:x "one" :y "two"}) => (throws))

(on-error! default-error-handler)
 

(fact "Default error handler returns nil"
  (with-out-str 
    (some-> (checked :Point {:x "one" :y "two"})
            (prn "successful stuff")))
  =not=> #"successful")

(fact "default success handler returns the original value"
  (checked :Point {:x 1 :y 2}) => {:x 1 :y 2})


;;; Multiple predicates

(type! :Point {:x [integer? pos?] :y [integer? pos?]})

(fact
  (let [result (with-out-str (checked :Point {:x -1 :y 2}))]
    result => #":x should be `pos\?`"))


(fact "map descriptions leave keys optional"
  (map #(checked :Point %) [{} {:x 1} {:y 1} {:x 1 :y 2}])
  => [{} {:x 1} {:y 1} {:y 2, :x 1}])



;;; Required keys

(type! :Point {:x [required-key integer?]
               :y [required-key integer?]})

(fact
  (let [result (with-out-str (checked :Point {:x "1"}))]
    result => #":y must exist and be non-nil"
    result => #":x should be `integer"))

(type! :Point
       [:x :y]
       {:x integer? :y integer?})

(fact 
  (let [result (with-out-str (checked :Point {:x "1"}))]
    result => #":y must exist and be non-nil"
    result => #":x should be `integer"))

(fact "excess keys are fine"
  (map #(checked :Point %) [{:x 1 :y 2}
                            {:x 1 :y 2 :z 3}
                            {:x 1 :y 2 :color "red"}])
  => [{:y 2, :x 1} {:y 2, :z 3, :x 1} {:y 2, :color "red", :x 1}])


;;; Combining types


(type! :ColorfulPoint
          {:x [required-key integer?]
           :y [required-key integer?]
           :color [required-key string?]})

(fact
  (let [result (with-out-str (checked :ColorfulPoint {:y 1 :color 1}))]
    result => #":x must exist"
    result => #":color should be `string"))


(type! :ColorfulPoint
              (includes :Point)
              {:color string?})
(fact
  (let [result (with-out-str (checked :ColorfulPoint {:y 1 :color 1}))]
    result => #":x must exist"
    result => #":color should be `string"))

(type! :Colorful {:color [required-key string?]})
(type! :ColorfulPoint (includes :Point) (includes :Colorful))

(fact
  (let [result (with-out-str (checked :ColorfulPoint {:y 1 :color 1}))]
    result => #":x must exist"
    result => #":color should be `string"))

(fact "combining types at check time"
  (let [result (with-out-str (checked [:Colorful :Point] {:y 1 :color 1}))]
    result => #":x must exist"
    result => #":color should be `string"))


;;; Nesting types and key paths

(def ok-figure {:color "red"
                :points [{:x 1, :y 1}
                         {:x 2, :y 3}]})


(type! :Figure (includes :Colorful)
               {[:points ALL :x] [required-key integer?]
                [:points ALL :y] [required-key integer?]})


(fact 
  (checked :Figure ok-figure) => ok-figure

  (let [result (with-out-str (checked :Figure {:points [{:y 1} {:x 1 :y "2"}]}))]
    result => #":color must exist"
    result => #"\[:points ALL :x\]\[0\] must exist"
    result => #"\[:points ALL :y\]\[1\] should be `integer\?`"
    ;; check sort order
    result => #"(?s)\[0\].+\[1\]"))

(fact "an ugly result"
  (let [result (with-out-str (checked :Figure {:points {:x 1 :y 2}}))]
    (count (str/split result #"\n")) => 5))
    
(fact "better"
  (let [result (with-out-str (checked :Figure {:points 3}))]
    result => #":color must exist"
    result => #"\[:points ALL :x\] is not a path into `\{:points 3\}`"
    result => #"\[:points ALL :y\] is not a path into `\{:points 3\}`"))
    

(start-over!)

