(ns structural-typing.guts.type-descriptions.f-canonicalizing
  (:require [structural-typing.guts.type-descriptions.canonicalizing :as subject]
            [structural-typing.guts.type-descriptions.m-ppps :as ppp]
            [structural-typing.guts.type-descriptions.m-maps :as m-map])
  (:use midje.sweet structural-typing.assist.testutil
        structural-typing.assist.special-words))


(def ps list) ; "partial type descriptions" - just to make top-level grouping easier to follow

(facts "Part 1: steps in canonicalization explained"
  (fact dc:validate-starting-descriptions
    (subject/dc:validate-starting-descriptions (ps {} [] even? :a)) => (ps {} [] even? :a)
    (subject/dc:validate-starting-descriptions (ps 1)) => (throws #"maps, functions, vectors, or keywords"))

  (fact dc:spread-collections-of-required-paths
    (fact "passes maps through unchanged"
      (subject/dc:spread-collections-of-required-paths (ps {} {:a 1})) => (just {} {:a 1}))
    
    (fact "splices vectors in"
      (subject/dc:spread-collections-of-required-paths
       (ps (requires [:l1a :l2a] [:l1b :l2b])
             {:c even?}))
      => (just [:l1a :l2a] [:l1b :l2b] {:c even?})))
  
  (fact "dc:keywords-to-required-maps converts a single key to a singleton path"
    (subject/dc:keywords-to-required-maps (ps :a {:c even?}))
    => (just [:a] {:c even?}))

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
      (subject/dc:flatten-maps (ps {:a {:b even?}})) => [ { [:a :b] [even?] } ]))

  (fact dc:allow-includes-in-preds
    (subject/dc:allow-includes-in-preds [{[:x] required-key}]) => [{[:x] required-key}]
    (subject/dc:allow-includes-in-preds [{[:x] [required-key]}]) => [{[:x] [required-key]}]
    (subject/dc:allow-includes-in-preds [{[:x] {:a 1}}]) => [{[:x] {:a 1}}]
    ;; The following is a bit ugly but empty maps are no-ops
    (subject/dc:allow-includes-in-preds [{[:x] [{:a 1}]}]) => [{[:x] {:a 1}} {}]
    (subject/dc:allow-includes-in-preds [{:x [required-key]}
                                         {:b [required-key {:a even?} odd?]}])
    => [{:x [required-key]}
        {:b {:a even?}}
        {:b [required-key odd?]}])
)







