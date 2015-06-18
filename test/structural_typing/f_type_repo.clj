(ns structural-typing.f-type-repo
  (:require [structural-typing.type-repo :as subject]
            [structural-typing.predicates :as p])
  (:require [com.rpl.specter :refer [ALL]])
  (:use midje.sweet))

(fact "various signifiers can be converted into a canonical form"
  (fact "signifiers that are maps"
    (fact "simple maps just have their keys and values vectorized"
      (subject/canonicalize {:a p/must-exist}) => {[:a] [p/must-exist]}
      (subject/canonicalize {:a p/must-exist, :b p/must-exist})
      => {[:a] [p/must-exist]
          [:b] [p/must-exist]})
    
    (fact "already vectorized keys and values are left alone"
      (subject/canonicalize {[:a] p/must-exist}) => {[:a] [p/must-exist]}
      (subject/canonicalize {[:a] [p/must-exist]}) => {[:a] [p/must-exist]}
      (subject/canonicalize {[:a :b] p/must-exist}) => {[:a :b] [p/must-exist]})

    (fact "nested maps"
      (subject/canonicalize {:a {:b p/must-exist}}) => {[:a :b] [p/must-exist]}))

  (fact "signifiers that are vectors"
    (fact "vectors are shorthand for maps with `p/must-exist`"
      (subject/canonicalize [ :a :b ]) => {[:a] [p/must-exist] [:b] [p/must-exist]})

    (fact "maps within vectors contribute paths - only paths - that must exist"
      (subject/canonicalize [ {:a #'even?, :b #'even?} ]) => {[:a] [p/must-exist]
                                                              [:b] [p/must-exist]}
      (subject/canonicalize [ [:a {:b {:c #'even?}
                                   :d #'even?}] ]) => {[:a :b :c] [p/must-exist]
                                                       [:a :d] [p/must-exist]}

      (subject/canonicalize [ [:a {:b #'even?} :e] ]) => (throws)))

  (fact "signifiers can only be maps and vectors"
    (subject/canonicalize :a) => (throws clojure.lang.ExceptionInfo #"must be a map or vector"))

  (fact "multiple arguments are allowed"
    (subject/canonicalize {:a p/must-exist} {:b p/must-exist}) => {[:a] [p/must-exist]
                                                                   [:b] [p/must-exist]}

    (fact "arguments with the same keys have their values merged"
      (subject/canonicalize {:a p/must-exist} {:a #'even?}) => {[:a] [p/must-exist #'even?]}
      (subject/canonicalize {:a {:b p/must-exist}}
                            {:a #'map?}
                            {:a {:b #'even?}}) => {[:a] [#'map?]
                                                   [:a :b] [p/must-exist #'even?]})

    (fact "maps and vectors can be merged"
      (subject/canonicalize [ :a :b ] {:a #'even?}) => {[:a] [p/must-exist #'even?]
                                                        [:b] [p/must-exist]}))

  (fact "And here's an example of various ways to talk about a nested structure"
    ;; A figure has a color and many points
    (subject/canonicalize [ :color
                           [:points ALL :x]
                           [:points ALL :y] ]) => {[:color] [p/must-exist]
                                                   [:points ALL :x] [p/must-exist]
                                                   [:points ALL :y] [p/must-exist]}

    (subject/canonicalize [:color
                           [:points ALL [:x :y]]]) => {[:color] [p/must-exist]
                                                       [:points ALL :x] [p/must-exist]
                                                       [:points ALL :y] [p/must-exist]}


    (fact "maps can contribute to the `required` list"
      (let [point {[:x] [#'integer?]
                   [:y] [#'integer?]}]
        ;; Here we require x and y to exist, but lose the requirement they be integers.
        (subject/canonicalize [:color [:points ALL point]])
        => {[:color] [p/must-exist]
            [:points ALL :x] [p/must-exist]
            [:points ALL :y] [p/must-exist]}
        
        ;; To get those options back, we use a map.
        (subject/canonicalize [:color [:points ALL point]]
                              {:color #'string?
                               [:points ALL] point})
        => {[:color] [p/must-exist #'string?]
            [:points ALL :x] [p/must-exist #'integer?]
            [:points ALL :y] [p/must-exist #'integer?]})


      ;; If a nested type has required keys, those are preserved.
      (let [required-xy-point {[:x] [p/must-exist #'integer?]
                               [:y] [p/must-exist #'integer?]}]
        ;; As above, a mention in the required list transfers required-ness
        (subject/canonicalize [:color [:points ALL required-xy-point]])
        => {[:color] [p/must-exist]
            [:points ALL :x] [p/must-exist]
            [:points ALL :y] [p/must-exist]}
        
        ;; To get those options back, we use a map.
        (subject/canonicalize [:color]
                              {:color #'string?
                               [:points ALL] required-xy-point})
        => {[:color] [p/must-exist #'string?]
            [:points ALL :x] [p/must-exist #'integer?]
            [:points ALL :y] [p/must-exist #'integer?]})))


  (fact "this is a pretty unlikely map to use, but it will work"
    (subject/canonicalize
       {:points {ALL {:x #'integer?
                      :y #'integer?}}}) => {[:points ALL :x] [#'integer?]
                                            [:points ALL :y] [#'integer?]})
  )









                  


                  
                  
