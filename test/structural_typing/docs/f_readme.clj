(ns structural-typing.docs.f-readme
 (:require [structural-typing.type :refer :all]
           [structural-typing.global-type :refer :all]
           [clojure.string :as str])
 (:use midje.sweet structural-typing.assist.testutil))


(start-over!)

(type! :Point {:x integer? :y integer?})

(fact
  (check-for-explanations :Point {:x "one" :y "two"})
  => (just #":x should be `integer.*one"
           #":y should be `integer.*two"))


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
  (check-for-explanations :Point {:x -1 :y 2})
  => (just #":x should be `pos\?`"))

(fact "map descriptions leave keys optional"
  (map #(checked :Point %) [{} {:x 1} {:y 1} {:x 1 :y 2}])
  => [{} {:x 1} {:y 1} {:y 2, :x 1}])



;;; Required keys

(type! :Point {:x [required-key integer?]
               :y [required-key integer?]})

(fact
  (check-for-explanations :Point {:x "1"})
  => (just #":x should be `integer"
           #":y must exist and be non-nil"))

(type! :Point
       [:x :y]
       {:x integer? :y integer?})

(fact
  (check-for-explanations :Point {:x "1"})
  => (just #":x should be `integer"
           #":y must exist and be non-nil"))

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
  (check-for-explanations :ColorfulPoint {:y 1 :color 1})
  => (just #":color should be `string"
           #":x must exist"))

(type! :ColorfulPoint
              (includes :Point)
              {:color string?})
(fact
  (check-for-explanations :ColorfulPoint {:y 1 :color 1})
  => (just #":color should be `string"
           #":x must exist"))

(type! :Colorful {:color [required-key string?]})
(type! :ColorfulPoint (includes :Point) (includes :Colorful))

(fact
  (check-for-explanations :ColorfulPoint {:y 1 :color 1})
  => (just #":color should be `string"
           #":x must exist"))

(fact "combining types at check time"
  (check-for-explanations [:Colorful :Point] {:y 1 :color 1})
  => (just #":color should be `string"
           #":x must exist"))

;;; Sequences of maps

(type! :Points {[ALL :x] integer?
                [ALL :y] integer?})
(type! :Points2 {[ALL] (includes :Point)})

(fact "incorrect paths"
  (check-for-explanations :Points 3) => (just #"\[ALL :x] is not a path into `3`"
                                              #"\[ALL :y] is not a path into `3`")

  (check-for-explanations :Points2 3) => (just #"\[ALL :x] is not a path into `3`"
                                               #"\[ALL :y] is not a path into `3`")

  (future-fact "Annoying side effect of there being no distinction between a present nil and a missing key"

    (check-for-explanations :Points [1 2 3]) => (just #"\[ALL :x] is not a path into `3`"
                                                      #"\[ALL :y] is not a path into `3`")))

;;; Nesting types and key paths

(def ok-figure {:color "red"
                :points [{:x 1, :y 1}
                         {:x 2, :y 3}]})


(type! :Figure (includes :Colorful)
               {[:points ALL :x] [required-key integer?]
                [:points ALL :y] [required-key integer?]})


(fact 
  (checked :Figure ok-figure) => ok-figure

  (check-for-explanations :Figure {:points [{:y 1} {:x 1 :y "2"}]})
  => (just #":color must exist"
           #"\[:points 0 :x\] must exist"
           #"\[:points 1 :y\] should be `integer\?`"))

(fact "a sorted result"
  (check-for-explanations :Figure {:points {:x 1 :y 2}})
  => (just ":color must exist and be non-nil"
           "[:points 0 :x] must exist and be non-nil"
           "[:points 0 :y] must exist and be non-nil"
           "[:points 1 :x] must exist and be non-nil"
           "[:points 1 :y] must exist and be non-nil"))
    
(fact "bad paths"
  (check-for-explanations :Figure {:points 3})
   => (just #":color must exist"
            #"\[:points ALL :x\] is not a path into `\{:points 3\}`"
            #"\[:points ALL :y\] is not a path into `\{:points 3\}`"))
    

(start-over!)

