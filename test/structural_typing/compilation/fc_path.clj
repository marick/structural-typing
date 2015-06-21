(ns structural-typing.compilation.fc-path
  (:require [structural-typing.compilation.c-path :as subject]
            [structural-typing.api.path :as path]
            [structural-typing.api.predicates :as pred])
  (:require [com.rpl.specter :refer [ALL]])
  (:use midje.sweet))

;;; The core

;; for a little clarity in the more complicated cases
(def type-has vector)


;; ..t.. is an irrelevant type-rep
(fact "type descriptions that are maps"
  (fact "simple maps just have their keys and values vectorized"
    (subject/compile ..t.. {:a pred/must-exist}) => {[:a] [pred/must-exist]}
    (subject/compile ..t.. {:a pred/must-exist, :b pred/must-exist})
    => {[:a] [pred/must-exist]
        [:b] [pred/must-exist]})
  
  (fact "already vectorized keys and values are left alone"
    (subject/compile ..t.. {[:a] pred/must-exist}) => {[:a] [pred/must-exist]}
    (subject/compile ..t.. {[:a] [pred/must-exist]}) => {[:a] [pred/must-exist]}
    (subject/compile ..t.. {[:a :b] pred/must-exist}) => {[:a :b] [pred/must-exist]})

  (fact "nested maps have keys flattened into paths"
    (subject/compile ..t.. {:a {:b pred/must-exist}}) => {[:a :b] [pred/must-exist]})

  (fact "this is a pretty unlikely map to use, but it will work"
    (subject/compile ..t..
                          {:points {ALL {:x #'integer?
                                         :y #'integer?}}}) => {[:points ALL :x] [#'integer?]
                                                               [:points ALL :y] [#'integer?]}))
  


(fact "type descriptions that are vectors"
  (fact "vectors are shorthand for maps with `pred/must-exist`"
    (subject/compile ..t.. [ :a :b ]) => {[:a] [pred/must-exist] [:b] [pred/must-exist]})
  (fact "alternate for above - for clarity"
    (subject/compile ..t.. (type-has :a :b)) => {[:a] [pred/must-exist] [:b] [pred/must-exist]})
  (fact "single-element vectors are the same as atoms"
    (subject/compile ..t.. (type-has [:a] [:b] ))
    => {[:a] [pred/must-exist] [:b] [pred/must-exist]})

  (fact "multi-element vectors are paths"
    (subject/compile ..t.. (type-has [:a :c]
                                          [:b :d])) => {[:a :c] [pred/must-exist]
                                                        [:b :d] [pred/must-exist]})
  
  (fact "nested and unnested elements can be combined"
    (subject/compile ..t.. (type-has :a [:b :c] :d)) => {[:a] [pred/must-exist]
                                                              [:b :c] [pred/must-exist]
                                                              [:d] [pred/must-exist]})

  (fact "double-nesting indicates alternatives"
    (subject/compile ..t.. (type-has :a [:b [1 2] :c] :d)) => {[:a] [pred/must-exist]
                                                                    [:b 1 :c] [pred/must-exist]
                                                                    [:b 2 :c] [pred/must-exist]
                                                                    [:d] [pred/must-exist]}

    (subject/compile ..t.. (type-has [[:a :b]])) => {[:a] [pred/must-exist]
                                                          [:b] [pred/must-exist]})


  (fact "maps within vectors contribute paths - only paths - that must exist"
    (subject/compile ..t.. [ {:a #'even?, :b #'even?} ]) => {[:a] [pred/must-exist]
                                                                  [:b] [pred/must-exist]}
    (subject/compile ..t.. [ [:a {:b {:c #'even?}
                                       :d #'even?}] ]) => {[:a :b :c] [pred/must-exist]
                                                           [:a :d] [pred/must-exist]}

      (subject/compile ..t.. [ [:a {:b #'even?} :e] ]) => (throws)))

(fact "signifiers can only be maps and vectors"
  (subject/compile ..t.. :a) => (throws #"maps or vectors"))

(fact "multiple arguments are allowed"
  (subject/compile ..t.. {:a pred/must-exist} {:b pred/must-exist}) => {[:a] [pred/must-exist]
                                                                       [:b] [pred/must-exist]}

  (fact "arguments with the same keys have their values merged"
    (subject/compile ..t.. {:a pred/must-exist} {:a #'even?}) => {[:a] [pred/must-exist #'even?]}
    (subject/compile ..t.. {:a {:b pred/must-exist}}
                          {:a #'map?}
                          {:a {:b #'even?}}) => {[:a] [#'map?]
                                                 [:a :b] [pred/must-exist #'even?]})

  (fact "maps and vectors can be merged"
    (subject/compile ..t.. [ :a :b ] {:a #'even?}) => {[:a] [pred/must-exist #'even?]
                                                            [:b] [pred/must-exist]}))


(fact "type descriptions that refer into a type map"
  (let [type-map {:type (subject/compile ..t.. [:a] {:a #'odd? :b #'even?})}]
    (subject/compile type-map (path/a :type)) => {[:a] [pred/must-exist #'odd?]
                                                          [:b] [#'even?]}

    (subject/compile type-map [ [:a (path/a :type) ]])  => {[:a :a] [pred/must-exist]
                                                                    [:a :b] [pred/must-exist]}

    (subject/compile type-map
                          {:a (path/a :type) }
                          {:a {:c #'pos?}}
                          [:c])
    => {[:a :a] [pred/must-exist #'odd?]
        [:a :b] [#'even?]
        [:a :c] [#'pos?]
        [:c] [pred/must-exist]}))

(fact "some typical uses of a type-map"
  (let [type-map (-> {}
                     (assoc :Point (subject/compile ..t.. [:x :y]
                                                         {:x #'integer? :y #'integer?})
                            :Colored (subject/compile ..t.. [:color]
                                                         {:color #'string?})
                            :OptionalColored (subject/compile ..t.. 
                                                                 {:color #'string?})))]
    (fact "merging types"
      (fact "making a colored point by addition"
        (subject/compile type-map (path/a :Point) [:color] {:color #'string?})
        => {[:color] [pred/must-exist #'string?]
            [:x] [pred/must-exist #'integer?]
            [:y] [pred/must-exist #'integer?]})
      
      (fact "or you can just merge types"
        (subject/compile type-map (path/a :Point) (path/a :Colored))
        => {[:color] [pred/must-exist #'string?]
            [:x] [pred/must-exist #'integer?]
            [:y] [pred/must-exist #'integer?]})
      
      (fact "note that merging an optional type doesn't make it required"
        (subject/compile type-map (path/a :Point) (path/a :OptionalColored))
        => {[:color] [#'string?]
            [:x] [pred/must-exist #'integer?]
            [:y] [pred/must-exist #'integer?]}))

    (fact "subtypes"
      (fact "making a colored point by addition"
        (subject/compile type-map (path/a :Point) [:color] {:color #'string?})
        => {[:color] [pred/must-exist #'string?]
            [:x] [pred/must-exist #'integer?]
            [:y] [pred/must-exist #'integer?]}
      
        (fact "or you can just merge types"
          (subject/compile type-map (path/a :Point) (path/a :Colored))
          => {[:color] [pred/must-exist #'string?]
              [:x] [pred/must-exist #'integer?]
              [:y] [pred/must-exist #'integer?]})
        
        (fact "note that merging an optional type doesn't make it required"
          (subject/compile type-map (path/a :Point) (path/a :OptionalColored))
          => {[:color] [#'string?]
              [:x] [pred/must-exist #'integer?]
              [:y] [pred/must-exist #'integer?]}))

      (fact "a line has a start and an end, which are points"
        (subject/compile type-map [:start :end]
                                       {:start (path/a :Point)
                                        :end (path/a :Point)})
        => {[:start] [pred/must-exist]
            [:end] [pred/must-exist]
            [:start :x] [pred/must-exist #'integer?]
            [:start :y] [pred/must-exist #'integer?]
            [:end :x] [pred/must-exist #'integer?]
            [:end :y] [pred/must-exist #'integer?]})

      (fact "a figure has a color and a set of points"
        (subject/compile type-map [:points]
                                       {[:points ALL] (path/a :Point)}
                                       (path/a :Colored))
        => {[:color] [pred/must-exist #'string?]
            [:points] [pred/must-exist]
            [:points ALL :x] [pred/must-exist #'integer?]
            [:points ALL :y] [pred/must-exist #'integer?]})

      (fact "noting that a figure has colored points"
        (subject/compile type-map [:points]
                                       {[:points ALL] (path/a :Point)}
                                       {[:points ALL] (path/a :Colored)})
        => {[:points] [pred/must-exist]
            [:points ALL :color] [pred/must-exist #'string?]
            [:points ALL :x] [pred/must-exist #'integer?]
            [:points ALL :y] [pred/must-exist #'integer?]}))))

(fact "And here, for posterity, of various non-'a' ways to talk about a nested structure"
  (let [point (subject/compile ..t.. {[:x] [#'integer?]
                                           [:y] [#'integer?]})]

    (fact "a little warm up without using any explicit notion of point"
      (subject/compile ..t.. [ :color
                                   [:points ALL :x]
                                   [:points ALL :y] ]) => {[:color] [pred/must-exist]
                                                           [:points ALL :x] [pred/must-exist]
                                                           [:points ALL :y] [pred/must-exist]}
                                   
      (subject/compile ..t.. [:color
                                   [:points ALL [:x :y]]]) => {[:color] [pred/must-exist]
                                                               [:points ALL :x] [pred/must-exist]
                                                               [:points ALL :y] [pred/must-exist]}


    (fact "Here we require x and y to exist, but without a requirement they be integers."
      (subject/compile ..t.. (type-has :color [:points ALL point]))
      => {[:color] [pred/must-exist]
          [:points ALL :x] [pred/must-exist]
          [:points ALL :y] [pred/must-exist]})

    (fact "Point predicates are gotten back using a map"
      (subject/compile ..t..
                            [:color [:points ALL point]]
                            {:color #'string?
                             [:points ALL] point})
      => {[:color] [pred/must-exist #'string?]
          [:points ALL :x] [pred/must-exist #'integer?]
          [:points ALL :y] [pred/must-exist #'integer?]})

    (fact "if a nested type has required keys, those are preserved."
      (let [required-xy-point {[:x] [pred/must-exist #'integer?]
                               [:y] [pred/must-exist #'integer?]}]
        ;; As above, a mention in the required list transfers required-ness
        (subject/compile ..t.. [:color [:points ALL required-xy-point]])
        => {[:color] [pred/must-exist]
            [:points ALL :x] [pred/must-exist]
            [:points ALL :y] [pred/must-exist]}
        
        ;; To get those options back, we use a map.
        (subject/compile ..t.. [:color]
                              {:color #'string?
                               [:points ALL] required-xy-point})
        => {[:color] [pred/must-exist #'string?]
            [:points ALL :x] [pred/must-exist #'integer?]
            [:points ALL :y] [pred/must-exist #'integer?]})))))




;;;; Utilities


(facts "nested-map->path-map flattens nested keys into a vector of keys"
  (fact "one argument form creates pathmap"
    (subject/nested-map->path-map {}) => {}
    (subject/nested-map->path-map {:a 1}) => {[:a] [1]}
    (subject/nested-map->path-map {:a {:b [1]}}) => {[:a :b] [1]}
    
    
    (subject/nested-map->path-map {:a {:b {:c 3 :d 4}
                                       :e {:f 5}
                                       :g 6}
                                   :h 7})
    => {[:a :b :c] [3]
        [:a :b :d] [4]
        [:a :e :f] [5]
        [:a :g] [6]
        [:h] [7]}
    
    (fact "vector keys are spliced into the path"
      (subject/nested-map->path-map {[:a :b] {:k "value"}
                                     :l1 {[:l2 :l3] [:pred1 :pred2]}})
      => {[:a :b :k] ["value"]
          [:l1 :l2 :l3] [:pred1 :pred2]}))

  (fact "two-argument form prepends a vector to all created paths"
    (subject/nested-map->path-map {:a 1} [:zz]) => {[:zz :a] [1]}
    (subject/nested-map->path-map {:a 1 :b {:c 2}} [:zz]) => {[:zz :a] [1]
                                                              [:zz :b :c] [2]}
    (subject/nested-map->path-map {:a 1 [:b :c] 2} [:zz]) => {[:zz :a] [1]
                                                              [:zz :b :c] [2]}))



(facts "`expand-path-shorthand` converts a single vector into a vector of paths."
        
  (fact "single-argument form"
    (subject/expand-path-shorthand [:x]) => [[:x]]
    (subject/expand-path-shorthand [:x :y]) => [[:x :y]]
    (subject/expand-path-shorthand [:x :y]) => [[:x :y]]
    
    (subject/expand-path-shorthand [:x [:y :z]]) => [[:x :y] [:x :z]]
    (subject/expand-path-shorthand [:a :b [:c :d]]) => [[:a :b :c] [:a :b :d]]
    
    (subject/expand-path-shorthand [:a :b [:c :d] :e]) => [[:a :b :c :e] [:a :b :d :e]]
    
    (subject/expand-path-shorthand [:a :b [:c :d] :e [:f :g]])
    => [[:a :b :c :e :f] 
        [:a :b :c :e :g] 
        [:a :b :d :e :f] 
        [:a :b :d :e :g] ]
    
    
    (fact "using maps for their keys"
      (subject/expand-path-shorthand [{:a #'even? :b #'even?}])
      => (just [:a] [:b] :in-any-order)
      
      (subject/expand-path-shorthand [:x :y {[:a :c] #'even? :b #'even?}])
      => (just [:x :y :a :c] [:x :y :b] :in-any-order)
      
      (subject/expand-path-shorthand [:x :y {:a {:c #'even?} :b #'even?}])
      => (just [:x :y :a :c] [:x :y :b] :in-any-order)
      
      (fact "such a map must be the last element in the vector"
        (subject/expand-path-shorthand [:x :y {[:a :c] #'even? :b #'even?} :z])
      => (throws #"The map must be the last element")))
    
    (fact "the embedded paths don't have to be vectors"
      (subject/expand-path-shorthand [:x (list :y :z)]) => [[:x :y] [:x :z]]))

  (fact "two argument forms"
    (subject/expand-path-shorthand [] [[]]) => [[]]
    (subject/expand-path-shorthand [:b] [[:a]]) => [[:a :b]]
    (subject/expand-path-shorthand [:b] [[1] [2]]) => [[1 :b] [2 :b]]

    (subject/expand-path-shorthand [{:a 1 :b 2}] [[1] [2]])
    => (just [1 :a] [1 :b] [2 :a] [2 :b] :in-any-order)

    (subject/expand-path-shorthand [:z {:a 1 :b 2}] [[1] [2]])
    => (just [1 :z :a] [1 :z :b] [2 :z :a] [2 :z :b] :in-any-order)

    (subject/expand-path-shorthand [:z {:a {:b 2}}] [[1]])
    => [[1 :z :a :b]]

    (subject/expand-path-shorthand [:a [:b :c] :d] [[1] [2]])
    => (just [1 :a :b :d] [1 :a :c :d] [2 :a :b :d] [2 :a :c :d] :in-any-order)

    (subject/expand-path-shorthand [:a [:b :c] [:d :e]] [[1]])
    => (just [1 :a :b :d] [1 :a :b :e] [1 :a :c :d] [1 :a :c :e])

    (subject/expand-path-shorthand [:a [:b :c] {:d 3 :e 4}] [[1]])
    => (just [1 :a :b :d] [1 :a :b :e] [1 :a :c :d] [1 :a :c :e])))
    

(facts "about handling paths that indicate required keys"
  (fact "returns a map"
    (subject/any-required-seq->maps {}) => {})
    
  (fact "the already compiled form turns into a simple map"
    (subject/any-required-seq->maps [[:a] [:b]]) => {[:a] [pred/must-exist]
                                                      [:b] [pred/must-exist]})

  (fact "top level non-collections are compiled"
    (subject/any-required-seq->maps [:a :b]) => {[:a] [pred/must-exist]
                                                 [:b] [pred/must-exist]}))

;;; Types can be expanded by signifier

(facts expand-type-finders
  (subject/expand-type-finders {} []) => []
  (subject/expand-type-finders {} [:x]) => [:x]

  (let [point {[:x] [#'even?], [:y] [#'odd?]}
        type-map {:Point point}]

    (subject/expand-type-finders type-map (path/a :Point)) => point
    (subject/expand-type-finders type-map [(path/a :Point)]) => [point]
    (subject/expand-type-finders type-map [:a [:b (path/a :Point)]]) => [:a [:b point]]

    (subject/expand-type-finders type-map {:a (path/a :Point)}) => {:a point}
    (subject/expand-type-finders type-map {:a {:b (path/a :Point)}}) => {:a {:b point}}
    (subject/expand-type-finders type-map [:a {:b (path/a :Point)}]) => [:a {:b point}]))





















;; (future-fact "three-argument form allows lookup in a type-map"
;;     (let [type-map {:Point {[:x] [pred/must-exist #'integer?]
;;                             [:y] [pred/must-exist #'integer?]}}]

;;       (subject/nested-map->path-map {:a (path/a :Point)} [:zz] type-map)
;;       => {[:zz :a :x] [pred/must-exist #'integer?]
;;           [:zz :a :y] [pred/must-exist #'integer?]}

;;       (subject/nested-map->path-map {:a 1
;;                                      [:b :c] (path/a :Point)
;;                                      :d {:e (path/a :Point)}}
;;                                     [] type-map)
;;       => {[:a] [1]
;;           [:b :c :x] [pred/must-exist #'integer?]
;;           [:b :c :y] [pred/must-exist #'integer?]
;;           [:d :e :x] [pred/must-exist #'integer?]
;;           [:d :e :y] [pred/must-exist #'integer?]}))

                  
;;   (future-fact "the two-argument form allows a type-map"
;;     (subject/expand-path-shorthand :x {}) => [:x]

;;     (let [point {[:x] ["x-vec"] [:y] ["y-vec"]}
;;           type-map {:Point point}]

;;       (future-fact "degenerate cases - no top-level sequence"
;;         (subject/expand-path-shorthand (path/a :Point) type-map)
;;         => (subject/expand-path-shorthand point)

;;         (subject/expand-path-shorthand {:a 1 :b (path/a :Point)} type-map)
;;         => (subject/expand-path-shorthand {:a 1 :b point})

;;         (subject/expand-path-shorthand {:a 1 [:figure ALL] (path/a :Point)} type-map)
;;         => (subject/expand-path-shorthand {:a 1 [:figure ALL] point})

;;         (subject/expand-path-shorthand :a type-map)
;;         => (subject/expand-path-shorthand :a))

;;       (future-fact "within sequence"
;;         (subject/expand-path-shorthand [(path/a :Point)] type-map)
;;         => (subject/expand-path-shorthand [point])

;;         (subject/expand-path-shorthand [:x (path/a :Point)] type-map)
;;         => (subject/expand-path-shorthand [:x point])

;; ;        (prn  "      ===========")

;;         (subject/expand-path-shorthand [:x [:y (path/a :Point)]] type-map)
;;         =future=> (subject/expand-path-shorthand [:x [:y point]])

;;         )

        

;;       ;; (subject/expand-path-shorthand {:a {:b (path/a :Point)}}) type-map)
;;       ;; => (just [:a :b :x] [:a :b :y] :in-any-order)

;;       ;; (future-fact "type map used in sequence"
;;       ;;   (subject/expand-path-shorthand [:a (path/a :Point)] type-map)
;;       ;;   => (subject/expand-path-shorthand [:a point]))
        
;; ))

                  
                  
