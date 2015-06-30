(ns structural-typing.mechanics.fm-canonical
  (:require [structural-typing.mechanics.m-canonical :as subject]
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
    (subject/canonicalize ..t.. {:a even?}) => {[:a] [even?]}
    (subject/canonicalize ..t.. {:a even?, :b even?})
    => {[:a] [even?]
        [:b] [even?]})
  
  (fact "already vectorized keys and values are left alone if flat"
    (subject/canonicalize ..t.. {[:a] even?}) => {[:a] [even?]}
    (subject/canonicalize ..t.. {[:a] [even?]}) => {[:a] [even?]}
    (subject/canonicalize ..t.. {[:a :b] even?}) => {[:a :b] [even?]})

  (fact "forks are allowed in vectors"
    (subject/canonicalize ..t.. {[:a [:b1 :b2]] even?})
    => {[:a :b1] [even?]
        [:a :b2] [even?]}

    (subject/canonicalize ..t.. {[:a [:b1 :b2] :c [:d1 :d2]] even?})
    => {[:a :b1 :c :d1] [even?]
        [:a :b1 :c :d2] [even?]
        [:a :b2 :c :d1] [even?]
        [:a :b2 :c :d2] [even?]})

  (fact "note that forks allow the possibility of duplicate paths"
    (subject/canonicalize ..t.. {[:a :b1] pos?
                                 [:a [:b1 :b2]] even?})
    => (just {[:a :b1] (just [(exactly pos?) (exactly even?)] :in-any-order)
              [:a :b2] [even?]})

    (subject/canonicalize ..t.. {[:a :b] pos?
                                 [:a] {:b even?}})
    => (just {[:a :b] (just [(exactly pos?) (exactly even?)] :in-any-order)}))
                          

  (fact "nested maps have keys flattened into paths"
    (subject/canonicalize ..t.. {:a {:b even?}}) => {[:a :b] [even?]})

  (fact "nested keys can be forked paths, though that should be an unusual case"
    (subject/canonicalize ..t.. {[:a :b] {[:c [:d1 :d2]] even?}})
    => {[:a :b :c :d1] [even?]
        [:a :b :c :d2] [even?]})

  (fact "this is a pretty unlikely map to use, but it will work"
    (subject/canonicalize ..t..
                          {:points {ALL {:x #'integer?
                                         :y #'integer?}}}) => {[:points ALL :x] [#'integer?]
                                                               [:points ALL :y] [#'integer?]}))
  


(fact "type descriptions that are vectors"
  (fact "vectors are shorthand for maps with `pred/required-key`"
    (subject/canonicalize ..t.. [ :a :b ]) => {[:a] [pred/required-key] [:b] [pred/required-key]})
  (fact "alternate for above - for clarity"
    (subject/canonicalize ..t.. (type-has :a :b)) => {[:a] [pred/required-key] [:b] [pred/required-key]})
  (fact "single-element vectors are the same as atoms"
    (subject/canonicalize ..t.. (type-has [:a] [:b] ))
    => {[:a] [pred/required-key] [:b] [pred/required-key]})

  (fact "multi-element vectors are paths"
    (subject/canonicalize ..t.. (type-has [:a :c]
                                          [:b :d])) => {[:a :c] [pred/required-key]
                                                        [:b :d] [pred/required-key]})
  
  (fact "nested and unnested elements can be combined"
    (subject/canonicalize ..t.. (type-has :a [:b :c] :d)) => {[:a] [pred/required-key]
                                                              [:b :c] [pred/required-key]
                                                              [:d] [pred/required-key]})

  (fact "double-nesting indicates alternatives"
    (subject/canonicalize ..t.. (type-has :a [:b [1 2] :c] :d)) => {[:a] [pred/required-key]
                                                                    [:b 1 :c] [pred/required-key]
                                                                    [:b 2 :c] [pred/required-key]
                                                                    [:d] [pred/required-key]}

    (subject/canonicalize ..t.. (type-has [[:a :b]])) => {[:a] [pred/required-key]
                                                          [:b] [pred/required-key]})


  (fact "maps within vectors contribute paths - only paths - that must exist"
    (subject/canonicalize ..t.. [ {:a #'even?, :b #'even?} ]) => {[:a] [pred/required-key]
                                                                  [:b] [pred/required-key]}
    (subject/canonicalize ..t.. [ [:a {:b {:c #'even?}
                                       :d #'even?}] ]) => {[:a :b :c] [pred/required-key]
                                                           [:a :d] [pred/required-key]}

      (subject/canonicalize ..t.. [ [:a {:b #'even?} :e] ]) => (throws)))

(fact "signifiers can only be maps and vectors"
  (subject/canonicalize ..t.. :a) => (throws #"maps or vectors"))

(fact "multiple arguments are allowed"
  (subject/canonicalize ..t.. {:a pred/required-key} {:b pred/required-key}) => {[:a] [pred/required-key]
                                                                       [:b] [pred/required-key]}

  (fact "arguments with the same keys have their values merged"
    (subject/canonicalize ..t.. {:a pred/required-key} {:a #'even?}) => {[:a] [pred/required-key #'even?]}
    (subject/canonicalize ..t.. {:a {:b pred/required-key}}
                          {:a #'map?}
                          {:a {:b #'even?}}) => {[:a] [#'map?]
                                                 [:a :b] [pred/required-key #'even?]})

  (fact "maps and vectors can be merged"
    (subject/canonicalize ..t.. [ :a :b ] {:a #'even?}) => {[:a] [pred/required-key #'even?]
                                                            [:b] [pred/required-key]}))


(fact "type descriptions that refer into a type map"
  (let [type-map {:type (subject/canonicalize ..t.. [:a] {:a #'odd? :b #'even?})}]
    (subject/canonicalize type-map (path/includes :type)) => {[:a] [pred/required-key #'odd?]
                                                              [:b] [#'even?]}
    
    (subject/canonicalize type-map [ [:a (path/includes :type) ]])  => {[:a :a] [pred/required-key]
                                                                        [:a :b] [pred/required-key]}

    (subject/canonicalize type-map
                          {:a (path/includes :type) }
                          {:a {:c #'pos?}}
                          [:c])
    => {[:a :a] [pred/required-key #'odd?]
        [:a :b] [#'even?]
        [:a :c] [#'pos?]
        [:c] [pred/required-key]}))

(fact "some typical uses of a type-map"
  (let [type-map (-> {}
                     (assoc :Point (subject/canonicalize ..t.. [:x :y]
                                                         {:x #'integer? :y #'integer?})
                            :Colored (subject/canonicalize ..t.. [:color]
                                                         {:color #'string?})
                            :OptionalColored (subject/canonicalize ..t.. 
                                                                 {:color #'string?})))]
    (fact "merging types"
      (fact "making a colored point by addition"
        (subject/canonicalize type-map (path/includes :Point) [:color] {:color #'string?})
        => {[:color] [pred/required-key #'string?]
            [:x] [pred/required-key #'integer?]
            [:y] [pred/required-key #'integer?]})
      
      (fact "or you can just merge types"
        (subject/canonicalize type-map (path/includes :Point) (path/includes :Colored))
        => {[:color] [pred/required-key #'string?]
            [:x] [pred/required-key #'integer?]
            [:y] [pred/required-key #'integer?]})
      
      (fact "note that merging an optional type doesn't make it required"
        (subject/canonicalize type-map (path/includes :Point) (path/includes :OptionalColored))
        => {[:color] [#'string?]
            [:x] [pred/required-key #'integer?]
            [:y] [pred/required-key #'integer?]}))

    (fact "subtypes"
      (fact "making a colored point by addition"
        (subject/canonicalize type-map (path/includes :Point) [:color] {:color #'string?})
        => {[:color] [pred/required-key #'string?]
            [:x] [pred/required-key #'integer?]
            [:y] [pred/required-key #'integer?]}
      
        (fact "or you can just merge types"
          (subject/canonicalize type-map (path/includes :Point) (path/includes :Colored))
          => {[:color] [pred/required-key #'string?]
              [:x] [pred/required-key #'integer?]
              [:y] [pred/required-key #'integer?]})
        
        (fact "note that merging an optional type doesn't make it required"
          (subject/canonicalize type-map (path/includes :Point) (path/includes :OptionalColored))
          => {[:color] [#'string?]
              [:x] [pred/required-key #'integer?]
              [:y] [pred/required-key #'integer?]}))

      (fact "a line has a start and an end, which are points"
        (subject/canonicalize type-map [:start :end]
                                       {:start (path/includes :Point)
                                        :end (path/includes :Point)})
        => {[:start] [pred/required-key]
            [:end] [pred/required-key]
            [:start :x] [pred/required-key #'integer?]
            [:start :y] [pred/required-key #'integer?]
            [:end :x] [pred/required-key #'integer?]
            [:end :y] [pred/required-key #'integer?]})

      (fact "a figure has a color and a set of points"
        (subject/canonicalize type-map [:points]
                                       {[:points ALL] (path/includes :Point)}
                                       (path/includes :Colored))
        => {[:color] [pred/required-key #'string?]
            [:points] [pred/required-key]
            [:points ALL :x] [pred/required-key #'integer?]
            [:points ALL :y] [pred/required-key #'integer?]})

      (fact "noting that a figure has colored points"
        (subject/canonicalize type-map [:points]
                                       {[:points ALL] (path/includes :Point)}
                                       {[:points ALL] (path/includes :Colored)})
        => {[:points] [pred/required-key]
            [:points ALL :color] [pred/required-key #'string?]
            [:points ALL :x] [pred/required-key #'integer?]
            [:points ALL :y] [pred/required-key #'integer?]}))))

(fact "And here, for posterity, various non-'a' ways to talk about a nested structure"
  (let [point (subject/canonicalize ..t.. {[:x] [#'integer?]
                                           [:y] [#'integer?]})]

    (fact "a little warm up without using any explicit notion of point"
      (subject/canonicalize ..t.. [ :color
                                   [:points ALL :x]
                                   [:points ALL :y] ]) => {[:color] [pred/required-key]
                                                           [:points ALL :x] [pred/required-key]
                                                           [:points ALL :y] [pred/required-key]}
                                   
      (subject/canonicalize ..t.. [:color
                                   [:points ALL [:x :y]]]) => {[:color] [pred/required-key]
                                                               [:points ALL :x] [pred/required-key]
                                                               [:points ALL :y] [pred/required-key]}


    (fact "Here we require x and y to exist, but without a requirement they be integers."
      (subject/canonicalize ..t.. (type-has :color [:points ALL point]))
      => {[:color] [pred/required-key]
          [:points ALL :x] [pred/required-key]
          [:points ALL :y] [pred/required-key]})

    (fact "Point predicates are gotten back using a map"
      (subject/canonicalize ..t..
                            [:color [:points ALL point]]
                            {:color #'string?
                             [:points ALL] point})
      => {[:color] [pred/required-key #'string?]
          [:points ALL :x] [pred/required-key #'integer?]
          [:points ALL :y] [pred/required-key #'integer?]})

    (fact "if a nested type has required keys, those are preserved."
      (let [required-xy-point {[:x] [pred/required-key #'integer?]
                               [:y] [pred/required-key #'integer?]}]
        ;; As above, a mention in the required list transfers required-ness
        (subject/canonicalize ..t.. [:color [:points ALL required-xy-point]])
        => {[:color] [pred/required-key]
            [:points ALL :x] [pred/required-key]
            [:points ALL :y] [pred/required-key]}
        
        ;; To get those options back, we use a map.
        (subject/canonicalize ..t.. [:color]
                              {:color #'string?
                               [:points ALL] required-xy-point})
        => {[:color] [pred/required-key #'string?]
            [:points ALL :x] [pred/required-key #'integer?]
            [:points ALL :y] [pred/required-key #'integer?]})))))




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



(facts "`expand-path-with-forks` converts a single vector into a vector of paths."
        
  (fact "single-argument form"
    (subject/expand-path-with-forks [:x]) => [[:x]]
    (subject/expand-path-with-forks [:x :y]) => [[:x :y]]
    (subject/expand-path-with-forks [:x :y]) => [[:x :y]]
    
    (subject/expand-path-with-forks [:x [:y :z]]) => [[:x :y] [:x :z]]
    (subject/expand-path-with-forks [:a :b [:c :d]]) => [[:a :b :c] [:a :b :d]]
    
    (subject/expand-path-with-forks [:a :b [:c :d] :e]) => [[:a :b :c :e] [:a :b :d :e]]
    
    (subject/expand-path-with-forks [:a :b [:c :d] :e [:f :g]])
    => [[:a :b :c :e :f] 
        [:a :b :c :e :g] 
        [:a :b :d :e :f] 
        [:a :b :d :e :g] ]
    
    
    (fact "using maps for their keys"
      (subject/expand-path-with-forks [{:a #'even? :b #'even?}])
      => (just [:a] [:b] :in-any-order)
      
      (subject/expand-path-with-forks [:x :y {[:a :c] #'even? :b #'even?}])
      => (just [:x :y :a :c] [:x :y :b] :in-any-order)
      
      (subject/expand-path-with-forks [:x :y {:a {:c #'even?} :b #'even?}])
      => (just [:x :y :a :c] [:x :y :b] :in-any-order)
      
      (fact "such a map must be the last element in the vector"
        (subject/expand-path-with-forks [:x :y {[:a :c] #'even? :b #'even?} :z])
      => (throws #"The map must be the last element")))
    
    (fact "the embedded paths don't have to be vectors"
      (subject/expand-path-with-forks [:x (list :y :z)]) => [[:x :y] [:x :z]]))

  (fact "two argument forms"
    (subject/expand-path-with-forks [] [[]]) => [[]]
    (subject/expand-path-with-forks [:b] [[:a]]) => [[:a :b]]
    (subject/expand-path-with-forks [:b] [[1] [2]]) => [[1 :b] [2 :b]]

    (subject/expand-path-with-forks [{:a 1 :b 2}] [[1] [2]])
    => (just [1 :a] [1 :b] [2 :a] [2 :b] :in-any-order)

    (subject/expand-path-with-forks [:z {:a 1 :b 2}] [[1] [2]])
    => (just [1 :z :a] [1 :z :b] [2 :z :a] [2 :z :b] :in-any-order)

    (subject/expand-path-with-forks [:z {:a {:b 2}}] [[1]])
    => [[1 :z :a :b]]

    (subject/expand-path-with-forks [:a [:b :c] :d] [[1] [2]])
    => (just [1 :a :b :d] [1 :a :c :d] [2 :a :b :d] [2 :a :c :d] :in-any-order)

    (subject/expand-path-with-forks [:a [:b :c] [:d :e]] [[1]])
    => (just [1 :a :b :d] [1 :a :b :e] [1 :a :c :d] [1 :a :c :e])

    (subject/expand-path-with-forks [:a [:b :c] {:d 3 :e 4}] [[1]])
    => (just [1 :a :b :d] [1 :a :b :e] [1 :a :c :d] [1 :a :c :e])))
    

(facts "about handling paths that indicate required keys"
  (fact "returns a map"
    (subject/any-required-seq->maps {}) => {})
    
  (fact "the already canonicalized form turns into a simple map"
    (subject/any-required-seq->maps [[:a] [:b]]) => {[:a] [pred/required-key]
                                                      [:b] [pred/required-key]})

  (fact "top level non-collections are canonicalized"
    (subject/any-required-seq->maps [:a :b]) => {[:a] [pred/required-key]
                                                 [:b] [pred/required-key]}))

;;; Types can be expanded by signifier

(facts expand-type-finders
  (subject/expand-type-finders {} []) => []
  (subject/expand-type-finders {} [:x]) => [:x]

  (let [point {[:x] [#'even?], [:y] [#'odd?]}
        type-map {:Point point}]

    (subject/expand-type-finders type-map (path/includes :Point)) => point
    (subject/expand-type-finders type-map [(path/includes :Point)]) => [point]
    (subject/expand-type-finders type-map [:a [:b (path/includes :Point)]]) => [:a [:b point]]

    (subject/expand-type-finders type-map {:a (path/includes :Point)}) => {:a point}
    (subject/expand-type-finders type-map {:a {:b (path/includes :Point)}}) => {:a {:b point}}
    (subject/expand-type-finders type-map [:a {:b (path/includes :Point)}]) => [:a {:b point}]))













