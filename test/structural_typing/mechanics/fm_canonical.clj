(ns structural-typing.mechanics.fm-canonical
  (:require [structural-typing.mechanics.m-canonical :as subject]
            [structural-typing.api.path :as path]
            [structural-typing.api.predicates :as pred])
  (:require [com.rpl.specter :refer [ALL]])
  (:use midje.sweet))


;; Note: ..t.. is used as an irrelevant type-repo

;;; Utilities

(facts "`flatten-forked-path` converts a single vector into a vector of paths."
        
  (fact "single-argument form"
    (subject/flatten-forked-path [:x]) => [[:x]]
    (subject/flatten-forked-path [:x :y]) => [[:x :y]]
    (subject/flatten-forked-path [:x :y]) => [[:x :y]]
    
    (subject/flatten-forked-path [:x [:y :z]]) => [[:x :y] [:x :z]]
    (subject/flatten-forked-path [:a :b [:c :d]]) => [[:a :b :c] [:a :b :d]]
    
    (subject/flatten-forked-path [:a :b [:c :d] :e]) => [[:a :b :c :e] [:a :b :d :e]]
    
    (subject/flatten-forked-path [:a :b [:c :d] :e [:f :g]])
    => [[:a :b :c :e :f] 
        [:a :b :c :e :g] 
        [:a :b :d :e :f] 
        [:a :b :d :e :g] ]

    (fact "note that `forks` is a synonym"
      (subject/flatten-forked-path [:x (path/forks :y1 :y2)]) => [[:x :y1] [:x :y2]])
    
    (fact "maps should have been removed before this is called"
      (subject/flatten-forked-path [{:a even? :b even?}]) => (throws)
      (subject/flatten-forked-path [:x :y {[:a :c] even? :b even?}]) => (throws)
      (subject/flatten-forked-path [:x :y {:a {:c even?} :b even?}]) => (throws)
      (subject/flatten-forked-path [:x :y {[:a :c] even? :b even?} :z]) => (throws))

    (fact "the embedded paths don't have to be vectors"
      (subject/flatten-forked-path [:x (list :y :z)]) => [[:x :y] [:x :z]]))

  (fact "two argument forms"
    (subject/flatten-forked-path [] [[]]) => [[]]
    (subject/flatten-forked-path [:b] [[:a]]) => [[:a :b]]
    (subject/flatten-forked-path [:b] [[1] [2]]) => [[1 :b] [2 :b]]

    (subject/flatten-forked-path [:a [:b :c] [:d :e]] [[1]])
    => (just [1 :a :b :d] [1 :a :b :e] [1 :a :c :d] [1 :a :c :e])))




;;; Decompressors

;; Note: in tests below, I'll use `list` when I want to signify that a
;; collection of values correspond to the multiple condensed descriptions that
;; `named` accepts.

(fact dc:validate-description-types
  (fact "passes through many things unchanged"
    (subject/dc:validate-description-types (list {} [] )) => (list {} [] ))

  (fact "errors in canonicalize context"
    (subject/canonicalize ..t.. :a) => (throws #"maps or vectors")))


(fact dc-expand-type-signifiers
  (fact "passes through many things unchanged"
    (subject/dc:expand-type-signifiers ..repo.. []) => []
    (let [unchanged [ (path/requires :x [:y :z]) {:a even?} ]]
      (subject/dc:expand-type-signifiers ..repo.. unchanged) => unchanged))
  
  
  (fact "deep-traverses to find `includes`"
    (let [point {[:x] [even?], [:y] [odd?]}
          type-map {:Point point}]

      (subject/dc:expand-type-signifiers type-map (list {[:a :b] (path/includes :Point)} ))
      => [{[:a :b] point}]

      (subject/dc:expand-type-signifiers type-map (list (path/requires :x [:a (path/includes :Point)])))
      => [ (path/requires :x [:a point]) ]


      (subject/dc:expand-type-signifiers type-map [
         (path/requires :x (list :a (path/includes :Point)))
         (path/requires (list :a [:b :c] (path/includes :Point)))
         ]) => 
           [ 
            (path/requires :x [:a point])
            (path/requires [:a [:b :c] point])
           ]))

  (fact "affects canonicalization"
    (let [type-map {:Type (subject/canonicalize ..t.. [:a] {:a odd? :b even?})}]
      (subject/canonicalize type-map (path/includes :Type)) => {[:a] [pred/required-key odd?]
                                                                [:b] [even?]}
      
      (subject/canonicalize type-map (path/requires [:a (path/includes :Type) ]))
      => {[:a] [pred/required-key]
          [:a :a] [pred/required-key odd?]
          [:a :b] [even?]}
      
      (subject/canonicalize type-map
                            {:a (path/includes :Type) }
                            {:a {:c pos?}}
                            [:c])
      => {[:a :a] [pred/required-key odd?]
          [:a :b] [even?]
          [:a :c] [pos?]
          [:c] [pred/required-key]})))

(fact dc:spread-path-collections
  (fact "passes maps through unchanged"
    (subject/dc:spread-path-collections (list {} {:a 1} )) => (just {} {:a 1}))
    
  (fact "splicing-substitutes paths"
    (subject/dc:spread-path-collections (list (path/requires [:l1a :l2a] [:l1b :l2b]) ))
    => (just [:l1a :l2a] [:l1b :l2b]))
  
  (fact "converts a single key to a singleton path"
    (subject/dc:spread-path-collections (list (path/requires :a :b) ))
    => (just [:a] [:b] ))

  (fact "affects canonicalization"
    (subject/canonicalize ..t.. [:a :b :c]) 
    => (subject/canonicalize ..t.. [:a] [:b] [:c])))

    
(fact dc:split-paths-ending-in-maps
  (fact "doesn't care about maps or most vectors"
    (subject/dc:split-paths-ending-in-maps (list {} [:a :b] )) => (just {} [:a :b] ))

  (fact "paths ending in maps are split into a pure path and a map"
    ;; note that vector has already been spread.
    (subject/dc:split-paths-ending-in-maps (list [:a {:b 1}] ))
    => (just [:a] {[:a] {:b 1}} ))

  (fact "error cases in context of canonicalize"
    (subject/canonicalize ..t.. (path/requires [{:a 1}]))
    => (throws "A map cannot be the first element of a path: `[{:a 1}]`")

    (subject/canonicalize ..t.. (path/requires [:a {:a even?} :a]))
    => (throws #"Nothing may follow a map within a path"))

  (fact "affects canonicalization"
    (subject/canonicalize ..t.. (path/requires [:x {:a even?, :b even?} ]))
    => {[:x] [pred/required-key]
        [:x :a] [even?]
        [:x :b] [even?]}

    (subject/canonicalize ..t.. (path/requires [:a {:b {:c even?}
                                                    :d even?}] ))
    => {[:a] [pred/required-key]
        [:a :b :c] [even?]
        [:a :d] [even?]}))


(fact dc:flatten-maps
  (fact "doesn't care about sequences"
    (subject/dc:flatten-maps []) => []
    (subject/dc:flatten-maps (list [:a :b] [:a])) => (just [:a :b] [:a]))

  (fact "makes sure keys are converted to paths and values are vectorized"
    (subject/dc:flatten-maps (list {} {:a 1})) => (just {} {[:a] [1]})
    (subject/dc:flatten-maps (list {} {[:a] [1]})) => (just {} {[:a] [1]}))
  
  (fact "flattens sub-maps"
    (subject/dc:flatten-maps (list {:a {:b 1}}
                                   {[:c :d] {:e 1}}
                                   {:f {[:g :h] {:i [3 4]
                                                 [:j :k] 5}}}))
    => (just {[:a :b] [1]}
             {[:c :d :e] [1]}
             {[:f :g :h :i] [3 4]
              [:f :g :h :j :k] [5]}))
             

  (fact "when flattening causes duplicate paths, values are merged"
    (let [[result] (subject/dc:flatten-maps (list {[:a :b :c] 1
                                                   :a {[:b :c] 2}
                                                   [:a :b] {:c 3
                                                            :d 4}}))]
      (keys result) => (just [:a :b :c] [:a :b :d] :in-any-order)
      (result [:a :b :c]) => (just [1 2 3] :in-any-order)
      (result [:a :b :d]) => [4]))

  (fact "forking paths are left alone"
    (subject/dc:flatten-maps (list {[:a [:b :c] :d] 1}
                                   {[ [:b :c] ] {[[:d :e]] 2}}))
    => (just {[:a [:b :c] :d]   [1]}
             {[[:b :c] [:d :e]] [2]}))

  (fact "error cases in context of canonicalization"
    (subject/canonicalize ..t.. {[:a {:a 1}] pos?})
    => (throws #"A path used as a map key.*a 1"))
 
  (fact "affects canonicalization"
    (subject/canonicalize ..t.. {:a {:b even?}}) => {[:a :b] [even?]}

    (subject/canonicalize ..t.. {[:a :b] {[:c [:d1 :d2]] even?}})
    => {[:a :b :c :d1] [even?]
        [:a :b :c :d2] [even?]}

    (fact "this is a pretty unlikely map to use, but it will work"
      (subject/canonicalize ..t..
                            {:points {ALL {:x integer?
                                           :y integer?}}}) => {[:points ALL :x] [integer?]
                                                               [:points ALL :y] [integer?]})))
  
(fact dc:required-paths->maps
  (fact "doesn't care about maps"
    (subject/dc:required-paths->maps []) => []
    (subject/dc:required-paths->maps (list {:a 1} )) => (just {:a 1}))

  (fact "produces one map for each incoming vector"
    (subject/dc:required-paths->maps (list [:a] [:b :c])) => (just {[:a] [pred/required-key]}
                                                                {[:b :c] [pred/required-key]} ))

  (fact "forking paths are left alone"
    (subject/dc:required-paths->maps (list [:a [:b :c] :d]))
    => (just {[:a [:b :c] :d] [pred/required-key]}))

  (fact "affects canonicalization"
    (fact "singleton paths"
      (subject/canonicalize ..t.. [ :a :b ])
      => {[:a] [pred/required-key] [:b] [pred/required-key]}
      (subject/canonicalize ..t.. (path/requires :a :b))
      => {[:a] [pred/required-key] [:b] [pred/required-key]}
      (subject/canonicalize ..t.. (path/requires [:a] [:b] ))
      => {[:a] [pred/required-key] [:b] [pred/required-key]})

    (fact "multi-element vectors are paths"
      (subject/canonicalize ..t.. (path/requires [:a :c]
                                                 [:b :d])) => {[:a :c] [pred/required-key]
                                                               [:b :d] [pred/required-key]})
  
    (fact "nested and unnested elements can be combined"
      (subject/canonicalize ..t.. (path/requires :a [:b :c] :d)) => {[:a] [pred/required-key]
                                                                     [:b :c] [pred/required-key]
                                                                     [:d] [pred/required-key]})))

(fact dc:unfork-map-paths
  (fact "leaves flat paths alone"
    (subject/dc:unfork-map-paths []) => []
    (subject/dc:unfork-map-paths (list {[:a] [1]} )) => (just {[:a] [1]}))

  (fact "produces new keys for forking paths"
    (subject/dc:unfork-map-paths (list {[:a [:b1 :b2] :c] [1], [:simple] [2]}))
    => (just {[:a :b1 :c] [1]
              [:a :b2 :c] [1]
              [:simple] [2]}))

  (fact "when de-forking causes duplicate paths, values are merged"
    (let [[result] (subject/dc:unfork-map-paths (list {[:a :b1 :c] [1]
                                                     [:a [:b1 :b2] :c] [2]
                                                     [:a :b2 :c] [3]}))]
      (keys result) => (just [:a :b1 :c] [:a :b2 :c] :in-any-order)
      (result [:a :b1 :c]) => (just [1 2] :in-any-order)
      (result [:a :b2 :c]) => (just [2 3] :in-any-order)))

  (fact "affects canonicalization"
    (subject/canonicalize ..t.. (path/requires :a [:b [:l1 :l2] :c] :d))
    => {[:a] [pred/required-key]
        [:b :l1 :c] [pred/required-key]
        [:b :l2 :c] [pred/required-key]
        [:d] [pred/required-key]}
    
    (subject/canonicalize ..t.. (path/requires [[:a :b]])) => {[:a] [pred/required-key]
                                                               [:b] [pred/required-key]}))

(fact dc:add-required-subpaths
  (subject/dc:add-required-subpaths []) => []
  (fact "leaves non-required paths alone"
    (let [in [ {[:a ALL] [even?]} ]]
      (subject/dc:add-required-subpaths in) => in))
  (fact "leaves paths with only keys alone"
    (let [in [ {[:a :b] [pred/required-key]} ]]
      (subject/dc:add-required-subpaths in) => in))

  (tabular 
    (fact "adds new maps for subpaths of required paths"
      (let [original {?path [even? pred/required-key]}]
        (subject/dc:add-required-subpaths original)
        => (merge original ?additions)))
    ?path                ?additions
    [:a ALL]             {[:a] [pred/required-key]}
    [:a ALL :b]          {[:a] [pred/required-key]}
    [:a ALL :b ALL]      {[:a] [pred/required-key]
                          [:a ALL :b] [pred/required-key]}
    [:a ALL :b ALL :c]   {[:a] [pred/required-key]
                          [:a ALL :b] [pred/required-key]}
    [:a :b ALL :c]       {[:a :b] [pred/required-key]}
    [:a :b ALL :c :d]    {[:a :b] [pred/required-key]}
    [:a :b ALL ALL]      {[:a :b] [pred/required-key]}
    [:a :b ALL ALL :c]   {[:a :b] [pred/required-key]}
    )

  (fact "new keys are created"
    (subject/dc:add-required-subpaths {[:a ALL] [pred/required-key]})
    => {[:a ALL] [pred/required-key]
            [:a] [pred/required-key]})

  (fact "old keys have required-key added on"
    (subject/dc:add-required-subpaths {[:a] [vector?]
                                       [:a ALL] [pred/required-key]})
    => {[:a ALL] [pred/required-key]
        [:a] [vector? pred/required-key]})

  (fact "but it's not added on twice"
    (subject/dc:add-required-subpaths {[:a] [pred/required-key]
                                       [:a ALL] [pred/required-key]})
    => {[:a ALL] [pred/required-key]
        [:a] [pred/required-key]}

    (subject/dc:add-required-subpaths {[:a] [vector?]
                                       [:a ALL] [pred/required-key]
                                       [:a ALL :b] [pred/required-key]})
    => {[:a ALL] [pred/required-key]
        [:a ALL :b] [pred/required-key]
        [:a] [vector? pred/required-key]})
  


  (future-fact "affects canonicalization"

    (subject/canonicalize ..t.. (path/requires [:a ALL :c]
                                               [:b :f ALL])
                                {:a even?}
                                {[:b :f ALL] even?})
    => {[:a ALL :c] [pred/required-key]
        [:b :f ALL] [pred/required-key even?]
        [:a]        [even? pred/required-key]
        [:b :f]     [pred/required-key]}))



;;;; About type-map and path merging, which happens throughout

(fact "multiple arguments are allowed"
  (subject/canonicalize ..t.. {:a pred/required-key} {:b pred/required-key})
  => {[:a] [pred/required-key]
      [:b] [pred/required-key]})

(fact "arguments with the same keys have their values merged"
  (subject/canonicalize ..t.. {:a pred/required-key} {:a even?})
  => {[:a] [pred/required-key even?]}
  (subject/canonicalize ..t..
                        {:a {:b pred/required-key}}
                        {:a map?}
                        {:a {:b even?}})
  => {[:a] [map?]
      [:a :b] [pred/required-key even?]})

(fact "maps and vectors can be merged"
  (subject/canonicalize ..t.. [ :a :b ] {:a even?})
  => {[:a] [pred/required-key even?]
      [:b] [pred/required-key]})

(fact "note that forks allow the possibility of duplicate paths"
  (subject/canonicalize ..t.. {[:a :b1] pos?
                               [:a [:b1 :b2]] even?})
  => (just {[:a :b1] (just [(exactly pos?) (exactly even?)] :in-any-order)
            [:a :b2] [even?]})
  
  (subject/canonicalize ..t.. {[:a :b] pos?
                               [:a] {:b even?}})
  => (just {[:a :b] (just [(exactly pos?) (exactly even?)] :in-any-order)}))
                          

;;; Some typical uses of a type-map
(let [type-map (-> {}
                   (assoc :Point (subject/canonicalize ..t.. [:x :y]
                                                       {:x integer? :y integer?})
                          :Colored (subject/canonicalize ..t.. [:color]
                                                         {:color string?})
                          :OptionalColored (subject/canonicalize ..t.. 
                                                                 {:color string?})))]
  (fact "merging types"
    (fact "making a colored point by addition"
      (subject/canonicalize type-map (path/includes :Point) [:color] {:color string?})
      => {[:color] [pred/required-key string?]
          [:x] [pred/required-key integer?]
          [:y] [pred/required-key integer?]})
    
    (fact "or you can just merge types"
      (subject/canonicalize type-map (path/includes :Point) (path/includes :Colored))
      => {[:color] [pred/required-key string?]
          [:x] [pred/required-key integer?]
          [:y] [pred/required-key integer?]})
    
    (fact "note that merging an optional type doesn't make it required"
      (subject/canonicalize type-map (path/includes :Point) (path/includes :OptionalColored))
      => {[:color] [string?]
          [:x] [pred/required-key integer?]
          [:y] [pred/required-key integer?]}))
  
  (fact "subtypes"
    (fact "making a colored point by addition"
      (subject/canonicalize type-map (path/includes :Point) [:color] {:color string?})
      => {[:color] [pred/required-key string?]
          [:x] [pred/required-key integer?]
          [:y] [pred/required-key integer?]}
      
      (fact "or you can just merge types"
        (subject/canonicalize type-map (path/includes :Point) (path/includes :Colored))
        => {[:color] [pred/required-key string?]
            [:x] [pred/required-key integer?]
            [:y] [pred/required-key integer?]})
      
      (fact "note that merging an optional type doesn't make it required"
        (subject/canonicalize type-map (path/includes :Point) (path/includes :OptionalColored))
        => {[:color] [string?]
            [:x] [pred/required-key integer?]
            [:y] [pred/required-key integer?]}))
    
    (fact "a line has a start and an end, which are points"
      (subject/canonicalize type-map [:start :end]
                            {:start (path/includes :Point)
                             :end (path/includes :Point)})
      => {[:start] [pred/required-key]
          [:end] [pred/required-key]
          [:start :x] [pred/required-key integer?]
          [:start :y] [pred/required-key integer?]
          [:end :x] [pred/required-key integer?]
          [:end :y] [pred/required-key integer?]})
    
    (fact "a figure has a color and a set of points"
      (subject/canonicalize type-map [:points]
                            {[:points ALL] (path/includes :Point)}
                            (path/includes :Colored))
      => {[:color] [pred/required-key string?]
          [:points] [pred/required-key]
          [:points ALL :x] [pred/required-key integer?]
          [:points ALL :y] [pred/required-key integer?]})
    
    (fact "noting that a figure has colored points"
      (subject/canonicalize type-map [:points]
                            {[:points ALL] (path/includes :Point)}
                            {[:points ALL] (path/includes :Colored)})
      => {[:points] [pred/required-key]
          [:points ALL :color] [pred/required-key string?]
          [:points ALL :x] [pred/required-key integer?]
          [:points ALL :y] [pred/required-key integer?]})))


;;;; Let us not forget the simple cases

(fact "simple maps just have their keys and values vectorized"
  (subject/canonicalize ..t.. {:a even?}) => {[:a] [even?]}
  (subject/canonicalize ..t.. {:a even?, :b even?})
  => {[:a] [even?]
      [:b] [even?]})

(fact "simple vectors"
  (subject/canonicalize ..t.. [:a :b]) => {[:a] [pred/required-key]
                                           [:b] [pred/required-key]})
  

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

