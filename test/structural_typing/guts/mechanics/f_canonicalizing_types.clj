(ns structural-typing.guts.mechanics.f-canonicalizing-types
  (:require [structural-typing.guts.mechanics.canonicalizing-types :as subject]
            [structural-typing.guts.mechanics.m-ppps :as ppp]
            [structural-typing.guts.mechanics.m-maps :as m-map]
            [structural-typing.guts.preds.required-key :refer [required-key]])
  (:require [com.rpl.specter :refer [ALL]])
  (:require [structural-typing.guts.paths.substituting :refer [includes]]
            [structural-typing.guts.paths.readable :refer :all])
  (:use midje.sweet))


(def ps list) ; "partial type descriptions" - just to make top-level grouping easier to follow

(facts "Part 1: steps in canonicalization explained"
  (fact dc:expand-type-signifiers
    ;; only use of the type map ..repo.. used when it's irrelevant
    
    (fact "passes through many things unchanged"
      (subject/dc:expand-type-signifiers ..repo.. []) => []
      (let [unchanged (ps (requires :x [:y :z])
                          {:a even?})]
        (subject/dc:expand-type-signifiers ..repo.. unchanged) => unchanged))
    
    (fact "deep-traverses to find `includes`"
      (let [point {[:x] [even?], [:y] [odd?]}
            type-map {:Point point}]
        
        (subject/dc:expand-type-signifiers type-map (ps {[:a :b]
                                                         (includes :Point)} ))
        => [{[:a :b] point}]
        
        (subject/dc:expand-type-signifiers type-map
                                           (ps (requires :x [:a (includes :Point)])))
        => [ (requires :x [:a point]) ]
        
        
        (subject/dc:expand-type-signifiers type-map
                                           (ps (requires :x [:a (includes :Point)])
                                               {[:a [:b :c]] (includes :Point)}))
        => (ps (requires :x [:a point])
               {[:a [:b :c]] point}))))

  (fact dc:validate-starting-descriptions
    (subject/dc:validate-starting-descriptions (ps {} [] )) => (ps {} [])
    (subject/dc:validate-starting-descriptions (ps :a)) => (throws #"maps or vectors"))

  (fact dc:spread-collections-of-required-paths
    (fact "passes maps through unchanged"
      (subject/dc:spread-collections-of-required-paths (ps {} {:a 1})) => (just {} {:a 1}))
    
    (fact "splices vectors in"
      (subject/dc:spread-collections-of-required-paths
       (ps (requires [:l1a :l2a] [:l1b :l2b])
             {:c even?}))
      => (just [:l1a :l2a] [:l1b :l2b] {:c even?}))
  
    (fact "converts a single key to a singleton path"
      (subject/dc:spread-collections-of-required-paths (ps (requires :a :b)
                                                           {:c even?}))
      => (just [:a] [:b] {:c even?})))


  (fact dc:split-paths-ending-in-maps
    (fact "doesn't care about maps or most vectors"
      (subject/dc:split-paths-ending-in-maps (ps {} [:a :b] )) => (just {} [:a :b] ))
    
    (fact "paths ending in maps are split into a pure path and a map"
      (subject/dc:split-paths-ending-in-maps (ps [:a {:b 1}] ))
      => (just [:a]
               {[:a] {:b 1}})))
  

  (fact dc:required-paths->maps
    (subject/dc:required-paths->maps []) => []
    (fact "doesn't care about maps"
      (subject/dc:required-paths->maps (ps {:a 1} )) => (just {:a 1}))

    (fact "produces one map for each incoming vector"
      (subject/dc:required-paths->maps (ps [:a] [:b :c])) => (just {[:a] [required-key]}
                                                                   {[:b :c] [required-key]} ))

    (fact "forking paths are not processed yet"
      (subject/dc:required-paths->maps (ps [:a [:b :c] :d]))
      => (just {[:a [:b :c] :d] [required-key]})))


  (fact dc:flatten-maps
    (subject/dc:flatten-maps []) => []

    (fact "only cares about maps"
      (subject/dc:flatten-maps (ps [:a :b] [:a])) => (just [:a :b] [:a]))

    (fact "flattens individual maps"
      (subject/dc:flatten-maps (ps {:a {:b even?}})) => [ { [:a :b] [even?] } ])))




(facts "Part 2: examples of use in canonicalization"
  (fact dc:expand-type-signifiers
    (let [type-map {:Type (subject/canonicalize ..t.. [:a] {:a odd? :b even?})}]
      (subject/canonicalize type-map (includes :Type)) => {[:a] [required-key odd?]
                                                                [:b] [even?]}
      
      (subject/canonicalize type-map (requires [:a (includes :Type) ]))
      => {[:a] [required-key]
          [:a :a] [required-key odd?]
          [:a :b] [even?]}
      
      (subject/canonicalize type-map
                            {:a (includes :Type) }
                            {:a {:c pos?}}
                            [:c])
      => {[:a :a] [required-key odd?]
          [:a :b] [even?]
          [:a :c] [pos?]
          [:c] [required-key]}))

  (fact dc:spread-collections-of-required-paths
    (subject/canonicalize ..t.. (requires :a :b :c [:d :e]))
    => (subject/canonicalize ..t.. (requires :a)
                                   (requires [:b])
                                   (requires :c) 
                                   (requires [:d :e])))

  (fact dc:split-paths-ending-in-maps
    (subject/canonicalize ..t.. (requires [:x {:a even?, :b even?}]))
    => (subject/canonicalize ..t.. (requires [:x]) {:x {:a even?, :b even?}})

    (subject/canonicalize ..t.. (requires [:x {:a even?, :b even?} ]))
    => {[:x] [required-key]
        [:x :a] [even?]
        [:x :b] [even?]}

    (subject/canonicalize ..t.. (requires [:a {:b {:c even?}
                                                    :d even?}] ))
    => {[:a] [required-key]
        [:a :b :c] [even?]
        [:a :d] [even?]})

  (fact dc:required-paths->maps
    (subject/canonicalize ..t.. (requires :a [:b :d]) (requires :c))
    => {[:a] [required-key]
        [:b :d] [required-key]
        [:c] [required-key]})
  

  (fact "dc:flatten-maps"
    (subject/canonicalize ..t.. {:a {:b even?}}) => {[:a :b] [even?]}

    (subject/canonicalize ..t.. {[:a :b] {[:c [:d1 :d2]] even?}})
    => {[:a :b :c :d1] [even?]
        [:a :b :c :d2] [even?]}

    (fact "this is a pretty unlikely map to use, but it will work"
      (subject/canonicalize ..t..
                            {:points {ALL {:x integer?
                                           :y integer?}}})
      => {[:points ALL :x] [integer?]
          [:points ALL :y] [integer?]}))


  (fact dc:fix-forked-paths
    (subject/canonicalize ..t.. (requires :a [:b [:l1 :l2] :c] :d))
    => {[:a] [required-key]
        [:b :l1 :c] [required-key]
        [:b :l2 :c] [required-key]
        [:d] [required-key]}
    
    (subject/canonicalize ..t.. (requires [[:a :b]])) => {[:a] [required-key]
                                                               [:b] [required-key]})
  

  (fact dc:fix-required-paths-with-collection-selectors
    (subject/canonicalize ..t.. (requires [:a ALL :c]
                                               [:b :f ALL])
                                {:a even?}
                                {[:b :f ALL] even?})
    => {[:a ALL :c] [required-key]
        [:b :f ALL] [required-key even?] ; Note that this order is enforced
        [:a]        [required-key even?]
        [:b :f]     [required-key]}))



(facts "About type-map and path merging"

  (fact "multiple arguments are allowed"
    (subject/canonicalize ..t.. {:a required-key} {:b required-key})
    => {[:a] [required-key]
        [:b] [required-key]})
  
  (fact "arguments with the same keys have their values merged"
    (subject/canonicalize ..t.. {:a required-key} {:a even?})
    => {[:a] [required-key even?]}
    (subject/canonicalize ..t..
                          {:a {:b required-key}}
                          {:a map?}
                          {:a {:b even?}})
    => {[:a] [map?]
        [:a :b] [required-key even?]})

  (fact "maps and vectors can be merged"
    (subject/canonicalize ..t.. [ :a :b ] {:a even?})
    => {[:a] [required-key even?]
        [:b] [required-key]})

  (fact "nested and unnested elements can be combined"
    (subject/canonicalize ..t.. (requires :a [:b :c] :d)) => {[:a] [required-key]
                                                              [:b :c] [required-key]
                                                              [:d] [required-key]})
  (fact "note that forks allow the possibility of duplicate paths"
    (subject/canonicalize ..t.. {[:a :b1] pos?
                                 [:a [:b1 :b2]] even?})
    => (just {[:a :b1] (just [(exactly pos?) (exactly even?)] :in-any-order)
              [:a :b2] [even?]})
    
    (subject/canonicalize ..t.. {[:a :b] pos?
                                 [:a] {:b even?}})
    => (just {[:a :b] (just [(exactly pos?) (exactly even?)] :in-any-order)})))
  

(facts "Some typical uses of a type-map"
  (let [type-map (-> {}
                     (assoc :Point (subject/canonicalize ..t.. [:x :y]
                                                         {:x integer? :y integer?})
                            :Colored (subject/canonicalize ..t.. [:color]
                                                           {:color string?})
                            :OptionalColored (subject/canonicalize ..t.. 
                                                                   {:color string?})))]
    (fact "merging types"
      (fact "making a colored point by addition"
        (subject/canonicalize type-map (includes :Point) [:color] {:color string?})
        => {[:color] [required-key string?]
            [:x] [required-key integer?]
            [:y] [required-key integer?]})
      
      (fact "or you can just merge types"
        (subject/canonicalize type-map (includes :Point) (includes :Colored))
        => {[:color] [required-key string?]
            [:x] [required-key integer?]
            [:y] [required-key integer?]})
      
      (fact "note that merging an optional type doesn't make it required"
        (subject/canonicalize type-map (includes :Point) (includes :OptionalColored))
        => {[:color] [string?]
            [:x] [required-key integer?]
            [:y] [required-key integer?]}))
    
    (fact "subtypes"
      (fact "making a colored point by addition"
        (subject/canonicalize type-map (includes :Point) [:color] {:color string?})
        => {[:color] [required-key string?]
            [:x] [required-key integer?]
            [:y] [required-key integer?]}
        
        (fact "or you can just merge types"
          (subject/canonicalize type-map (includes :Point) (includes :Colored))
          => {[:color] [required-key string?]
              [:x] [required-key integer?]
              [:y] [required-key integer?]})
        
        (fact "note that merging an optional type doesn't make it required"
          (subject/canonicalize type-map (includes :Point) (includes :OptionalColored))
          => {[:color] [string?]
              [:x] [required-key integer?]
              [:y] [required-key integer?]}))
      
      (fact "a line has a start and an end, which are points"
        (subject/canonicalize type-map [:start :end]
                              {:start (includes :Point)
                               :end (includes :Point)})
        => {[:start] [required-key]
            [:end] [required-key]
            [:start :x] [required-key integer?]
            [:start :y] [required-key integer?]
            [:end :x] [required-key integer?]
            [:end :y] [required-key integer?]})
      
    (fact "a figure has a color and a set of points"
      (subject/canonicalize type-map [:points]
                            {[:points ALL] (includes :Point)}
                            (includes :Colored))
      => {[:color] [required-key string?]
          [:points] [required-key]
          [:points ALL :x] [required-key integer?]
          [:points ALL :y] [required-key integer?]})
    
    (fact "noting that a figure has colored points"
      (subject/canonicalize type-map [:points]
                            {[:points ALL] (includes :Point)}
                            {[:points ALL] (includes :Colored)})
      => {[:points] [required-key]
          [:points ALL :color] [required-key string?]
          [:points ALL :x] [required-key integer?]
          [:points ALL :y] [required-key integer?]}))))
  
  
(fact "let us not forget the simple cases"
  (fact "simple maps just have their keys and values vectorized"
    (subject/canonicalize ..t.. {:a even?}) => {[:a] [even?]}
    (subject/canonicalize ..t.. {:a even?, :b even?})
    => {[:a] [even?]
        [:b] [even?]})
  
  (fact "simple vectors"
    (subject/canonicalize ..t.. [:a :b]) => {[:a] [required-key]
                                             [:b] [required-key]}))
  
;;;; Canonicalize - random examples, probably redundant

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
