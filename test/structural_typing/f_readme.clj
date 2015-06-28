(ns structural-typing.f-readme
 (:require [structural-typing.type :refer :all]
           [structural-typing.global-type :refer :all])
 (:use midje.sweet))

(start-over!)

(type! :Point {:x integer? :y integer?})

(fact
  (let [result (with-out-str (checked :Point {:x "one" :y "two"}))]
    result => #":y should be `integer.*two"
    result => #":x should be `integer.*one"))

(on-error! throwing-error-handler)
(fact
  (checked :Point {:x "one" :y "two"}) => (throws))


 
(start-over!)
(type! :Point {:x integer? :y integer?})

(fact "Default error handler returns nil"
  (with-out-str 
    (some-> (checked :Point {:x "one" :y "two"})
            (prn "successful stuff")))
  =not=> #"successful stuff")

(fact "default success handler returns the original value"
  (checked :Point {:x 1 :y 2}) => {:x 1 :y 2})


(fact "map descriptions leave keys optional"
  (map #(checked :Point %) [{} {:x 1} {:y 1} {:x 1 :y 2}])
  => [{} {:x 1} {:y 1} {:y 2, :x 1}])



(fact "explicit required key in map"
  (start-over!)
  (type! :Point {:x [required-key integer?]
                              :y [required-key integer?]})

  (let [result (with-out-str (checked :Point {:x "1"}))]
    result => #":y must exist and be non-nil"
    result => #":x should be `integer"))


(fact "all predicates are checked"
  (type! :Whatever {:x [pos? even?]})
  (let [result (with-out-str (checked :Whatever {:x -1}))]
    result => #":x should be `pos"
    result => #":x should be `even"))

(fact "vector form for required keys"
  (type! :Point
                      [:x :y]
                      {:x integer? :y integer?})
  (with-out-str (checked :Point {:x 1})) => #":y must exist")

(fact "only required"
  (type! :Point [:x :y])
  (with-out-str (checked :Point {:x "one!"})) => #":y must exist and be non-nil")

(fact "excess keys are fine"
  (map #(checked :Point %) [{:x 1 :y 2}
                                 {:x 1 :y 2 :z 3}
                                 {:x 1 :y 2 :color "red"}])
  => [{:y 2, :x 1} {:y 2, :z 3, :x 1} {:y 2, :color "red", :x 1}])

(facts "many ways to create types"
  (fact "explicit"
    (type! :ColorfulPoint
                        [:x :y :color]
                        {:x integer? :y integer? :color string?})
    (let [result (with-out-str (checked :ColorfulPoint {:y 1 :color 1}))]
      result => #":x must exist"
      result => #":color should be `string"))


  (fact "like type extension"
    (type! :ColorfulPoint
                        (includes :Point) ; "base type"
                        [:color]               ; new required keys
                        {:color string?})      ; what we want to insist about our colors
    (let [result (with-out-str (checked :ColorfulPoint {:y 1 :color 1}))]
      result => #":x must exist"
      result => #":color should be `string"))

  (fact "mixins"
    (type! :Colorful [:color] {:color string?})
    (type! :ColorfulPoint (includes :Point) (includes :Colorful))
    (let [result (with-out-str (checked :ColorfulPoint {:y 1 :color 1}))]
      result => #":x must exist"
      result => #":color should be `string"))
  
  (fact "combining types at check time"
    (let [result (with-out-str (checked [:Colorful :Point] {:y 1 :color 1}))]
      result => #":x must exist"
      result => #":color should be `string"))
)




(start-over!)

(fact "versions of a figure"
  (let [bad-figure {:colr-typo "red"
                    :points [ {:x 1}
                              {:y 2}]}]
    (fact "explicit"
      (type! :Figure
                          [:color [:points ALL :x]
                                  [:points ALL :y]])
      (let [result (with-out-str (checked :Figure bad-figure))]
        result => #":color must exist"
        result => #"\[:points ALL :x\]\[1\] must exist"
        result => #"\[:points ALL :y\]\[0\] must exist"
        ;; check sort order
        result => #"(?s)\[0\].+\[1\]"))
    
    (fact "taking required values from previous types"
      (type! :Point [:x :y] {:x integer? :y integer?})
      (type! :Colorful [:color] {:color string?})
      (type! :Figure [(includes :Colorful)
                                   [:points ALL (includes :Point)]])
      (let [result (with-out-str (checked :Figure bad-figure))]
        result => #":color must exist"
        result => #"\[:points ALL :x\]\[1\] must exist"
        result => #"\[:points ALL :y\]\[0\] must exist"
        ;; check sort order
        result => #"(?s)\[0\].+\[1\]"))

    (fact "taking required values from previous types"
      (type! :Figure (requires (includes :Colorful)
                               [:points ALL (includes :Point)]))
      (let [result (with-out-str (checked :Figure bad-figure))]
        result => #":color must exist"
        result => #"\[:points ALL :x\]\[1\] must exist"
        result => #"\[:points ALL :y\]\[0\] must exist"
        ;; check sort order
        result => #"(?s)\[0\].+\[1\]"))


)

)
    
