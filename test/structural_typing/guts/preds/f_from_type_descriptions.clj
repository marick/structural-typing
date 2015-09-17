(ns structural-typing.guts.preds.from-type-descriptions
  (:require [com.rpl.specter :as specter])
  (:require [structural-typing.guts.preds.from-type-descriptions :as subject]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.preds :as preds])
  (:use midje.sweet
        structural-typing.assist.special-words))

(fact "variations in path processing - in the path given to specter"
  (fact "wildcard paths"
    (let [variation (subject/capture-path-variation [ALL :x] [even?])]
      (subject/run-specter variation [{:x 111} {:x 222}])
      => (contains {:path [ALL :x]
                    :whole-value [{:x 111} {:x 222}]
                    :leaf-values [ 111 222 ]
                    :path-adjustments [ [0] [1] ]})))

  (fact "single result paths"
    (let [variation (subject/capture-path-variation [:x] [even?])]
      (subject/run-specter variation {:x 111})
      => (contains {:path [:x]
                    :whole-value {:x 111}
                    :leaf-values [ 111 ]}))))

(fact "spreading leaf values into many entities with a leaf value"
  (fact "wildcard paths"
    (let [variation (subject/capture-path-variation [ALL :x] [even?])
          in {:leaf-values [..one.. ..two..] :path-adjustments [..p1.. ..p2..] :extra ..extra..}]
      (subject/spread-leaf-values variation in)
      => [(assoc in :leaf-value ..one.. :path-adjustment ..p1..)
          (assoc in :leaf-value ..two.. :path-adjustment ..p2..)]))

  (fact "single result paths"
    (let [variation (subject/capture-path-variation [:x] [even?])
          in {:leaf-values [..one.. ..two..] :extra ..extra..}]
      (subject/spread-leaf-values variation in)
      => [(assoc in :leaf-value ..one..)
          (assoc in :leaf-value ..two..)])))

(fact "running predicates"
  (fact "wildcard paths"
    (let [variation (subject/capture-path-variation [ALL :x] [even? pos?])
          whole-value [{:x 1} {:x 2} {:x -1}]]
      (->> (subject/run-specter variation whole-value)
           (subject/process-specter-results variation)
           (subject/spread-leaf-values variation)
           (subject/run-preds variation))
      => (just (contains {:whole-value whole-value
                          :leaf-value 1
                          :path [0 :x]
                          :predicate-string "even?"})
               ;; Note that element 1 is missing
               (contains {:whole-value whole-value
                          :leaf-value -1
                          :path [2 :x]
                          :predicate-string "even?"})
               (contains {:whole-value whole-value
                          :leaf-value -1
                          :path [2 :x]
                          :predicate-string "pos?"}))))

  (fact "single result paths"
    (let [variation (subject/capture-path-variation [:x] [even?])]
      (->> (subject/run-specter variation {:x 1})
           (subject/process-specter-results variation)
           (subject/spread-leaf-values variation)
           (subject/run-preds variation))
      => (just (contains {:whole-value {:x 1}
                          :leaf-value 1
                          :path [:x]
                          :predicate-string "even?"})))))
    

(defn run-preds [preds input]
  ((subject/compile-predicates preds) input))

(fact "compile multiple predicates into a function that checks each of them"
  (let [input {:leaf-value 1 :whole-value {:x 1} :path [:x]}
        oopsies (run-preds [even? odd?] input)]
    oopsies => (just (contains (assoc input :predicate (exactly even?))))
    (oopsie/explanations oopsies) => [":x should be `even?`; it is `1`"])

  (let [input {:leaf-value -3 :whole-value {[:x :y] -3} :path [:x :y]}
        oopsies (run-preds [pos? #'even?] input)]
    oopsies => (just (contains (assoc input :predicate (exactly pos?)))
                    (contains (assoc input :predicate (exactly #'even?))))
    ;; Note they are sorted.
    (oopsie/explanations oopsies) => ["[:x :y] should be `even?`; it is `-3`"
                                      "[:x :y] should be `pos?`; it is `-3`"])

  (let [input {:leaf-value -3 :whole-value {[:x :y] -3} :path [:x :y]}
        oopsies (run-preds [(->> pos? (show-as "POS!"))] input)]
    (oopsie/explanations oopsies) => ["[:x :y] should be `POS!`; it is `-3`"])

  (let [input {:leaf-value "string" :whole-value {[:x] "string"} :path [:x]}
        oopsies (run-preds [pos?] input)]
    (oopsie/explanations oopsies) => [":x should be `pos?`; it is `\"string\"`"]))

(defn ->checker [& elts]
  (subject/lift elts {}))

(fact "handling ALL"
  (let [checker (->checker [[:points ALL :x]]
                           {[:points ALL :x] even?})]
    (oopsie/explanations (checker {:points [{:x 11} {:x 22} {:x 33} {:y 111}]}))
    => (just "[:points 0 :x] should be `even?`; it is `11`"
             "[:points 2 :x] should be `even?`; it is `33`"
             "[:points 3 :x] must exist and be non-nil"
             :in-any-order)))

  (fact "a path with multiple values (RANGE)"
    (fact "simple case"
      (let [checker (->checker {[(RANGE 2 4)] even?})]
        (fact "a range avoids broken values"
          (oopsie/explanations (checker [:wrong :wrong 2 4 :wrong])) => empty?)
        
        (fact "... but allows processing of values within the range"
          (oopsie/explanations (checker [:wrong :wrong 0 41 :wrong]))
          => (just "[3] should be `even?`; it is `41`"))))
    
    (fact "multiple ranges"
      (let [checker (->checker {[(RANGE 2 4) (RANGE 1 2)] even?})]
        (fact "a range avoids broken values"
          (oopsie/explanations (checker [:wrong :wrong [:wrong 2 :wrong] [:wrong 4] :wrong])) => empty?)
        
        (fact "... but allows processing of values within the range"
          (oopsie/explanations (checker [:wrong :wrong [:wrong 1 :wrong] [:wrong 3]]))
          => (just "[2 1] should be `even?`; it is `1`"
                   "[3 1] should be `even?`; it is `3`"))))
    
    (fact "combination of RANGE and ALL"
      (let [checker (->checker {[(RANGE 2 4) ALL] even?})]
        (fact "a range avoids broken values"
          (oopsie/explanations (checker [:wrong :wrong [2 4 6] [4] :wrong])) => empty?)
        
        (fact "... but allows processing of values within the range"
          (oopsie/explanations (checker [:wrong :wrong [0 1 2] [2 3]]))
          => (just "[2 1] should be `even?`; it is `1`"
                   "[3 1] should be `even?`; it is `3`"))))
    
    (fact "including other path elements"
      (let [checker (->checker {[:a (RANGE 1 3) :b (RANGE 1 5) pos?] even?})]
        (fact "a range avoids broken values"
          (oopsie/explanations (checker {:a [:wrong 
                                                  {:b [1  2  2  2  2 1]}
                                                  {:b [1 -1 -1 -1 -1 1]}
                                                  :wrong]})) => empty?)
        
        (fact "... but allows processing of values within the range"
          (oopsie/explanations (checker {:a [:wrong
                                                  {:b [1  2  2  2  3 1]}
                                                  {:b [1 -1 -1  5 -1 1]}
                                                  :wrong]}))
          => (just "[:a 1 :b 4 pos?] should be `even?`; it is `3`"
                   "[:a 2 :b 3 pos?] should be `even?`; it is `5`")))))


(fact "compiling a whole type"
  (fact "Simple case"
    (let [checker (->checker [:a])]
      (oopsie/explanations (checker {})) => (just ":a must exist and be non-nil")))

  (fact "An optional value"
    (let [odd-if-exists (->checker {:a odd?})]
      (oopsie/explanations (odd-if-exists {})) => empty?
      (oopsie/explanations (odd-if-exists {:a 2})) => (just ":a should be `odd?`; it is `2`")
      (oopsie/explanations (odd-if-exists {:a 3})) => empty?)

    (fact "Note the difference from a required value"
      (let [odd-and-exists (->checker [:a] {:a odd?})]
        (oopsie/explanations (odd-and-exists {})) => (just ":a must exist and be non-nil")
        (oopsie/explanations (odd-and-exists {:a 2})) => (just ":a should be `odd?`; it is `2`")
        (oopsie/explanations (odd-and-exists {:a 3})) => empty?)))

  (fact "a path"
    (let [odd-and-exists (->checker [[:a :b]] {:a {:b odd?}})]
      (oopsie/explanations (odd-and-exists {})) => (just "[:a :b] must exist and be non-nil")
      (oopsie/explanations (odd-and-exists {:a "hi"})) => (just "[:a :b] must exist and be non-nil")
      (oopsie/explanations (odd-and-exists {:a {:b 2}})) => (just "[:a :b] should be `odd?`; it is `2`")
      (oopsie/explanations (odd-and-exists {:a {:b 3}})) => empty?))
  
  (fact "multiple paths in the type"
    (let [checker (->checker {:color string?
                              :point {:x integer?
                                      :y integer?}})]
      (oopsie/explanations (checker {})) => empty? ; all optional
      (oopsie/explanations (checker {:color "green"})) => empty?
      (oopsie/explanations (checker {:color 1})) => (just ":color should be `string?`; it is `1`")
      (oopsie/explanations (checker {:color "green"
                           :point {:x "1"
                                   :y "2"}}))
      => (just "[:point :x] should be `integer?`; it is `\"1\"`"
               "[:point :y] should be `integer?`; it is `\"2\"`"
               :in-any-order)))

  (fact "the whole type can be checked"
    (let [checker (->checker even?)]
      (checker 2) => []
      (oopsie/explanations (checker 1)) => (just "Value should be `even?`; it is `1`"))

    (let [checker (->checker (show-as "evencount" (comp even? count)))]
      (oopsie/explanations (checker {:a 1})) => (just "Value should be `evencount`; it is `{:a 1}`"))
    
    (let [checker (->checker (show-as "evencount" (comp even? count))
                             {:a [required-key even?]})]
      (oopsie/explanations (checker {:a 1}))
      => (just ":a should be `even?`; it is `1`"
               "Value should be `evencount`; it is `{:a 1}`"))
))
        


(fact "a path that can't be applied produces an error"
  (fact "ALL"
    (let [checker (->checker [[:x ALL :y]])]
      (checker {:x 1}) => (just (contains {:path [:x ALL :y]
                                                :whole-value {:x 1}}))
      (oopsie/explanations (checker {:x 1})) => (just #"\[:x ALL :y\] is not a path into `\{:x 1\}`")
      (oopsie/explanations (checker {:x :a})) => (just #"\[:x ALL :y\] is not a path into `\{:x :a\}`")
      
      (fact "these are fine, though"
        (oopsie/explanations (checker {:x [0]})) => (just "[:x 0 :y] must exist and be non-nil")
        (checker {:x []}) => empty?)
      
      (fact "A path containing an array complains if prefix doesn't exist"
        (oopsie/explanations (checker {})) => (just #":x must exist"))
      
      (fact "an unfortunate side effect of strings being collections"
        (oopsie/explanations (checker {:x "string"}))
        => (contains "[:x 0 :y] must exist and be non-nil"))))

  (fact "RANGE"
    (fact "ending index just fits"
      (let [checker (->checker {[(RANGE 1 3)] even?})]
        (oopsie/explanations (checker [0 2 4])) => empty?))

    (fact "ending index comes too soon"
      (let [checker (->checker {[(RANGE 1 3)] even?})]
        (oopsie/explanations (checker [0 2]))
        => (just #"\[\(RANGE 1 3\)\] is not a path into `\[0 2\]`")))

    (fact "starting index comes too soon"
      (let [checker (->checker {[(RANGE 2 5)] even?})]
        (oopsie/explanations (checker [0 2]))
        => (just #"\[\(RANGE 2 5\)\] is not a path into \`\[0 2\]\`")))

    (fact "in a previous bug, multiple range expressions all printed with same value"
      (let [checker (->checker {[:a (RANGE 1 4) :b (RANGE 1 5) pos?] even?})]
        (oopsie/explanations (checker {:a [:wrong :wrong
                                                {:b [1  2  2  2  2 1]}
                                                {:b [1 -1 -1 -1 -1 1]}
                                                :wrong]}))
        => (just #"\[:a \(RANGE 1 4\) :b \(RANGE 1 5\) pos\?\] is not a path")))))


(fact path-will-match-many?
  (subject/path-will-match-many? [:a :b]) => false
  (subject/path-will-match-many? [:a ALL :b]) => true)

(fact replace-with-indices
  (fact "ALL needn't worry about offsets"
    (subject/replace-with-indices [ALL ALL] [17 3]) => [17 3]
    (subject/replace-with-indices [:a ALL :b ALL] [17 3]) => [:a 17 :b 3])
  (fact "... and, as it happens, RANGE needn't either"
    (subject/replace-with-indices [(RANGE 3 100) ALL] [17 3]) => [17 3]
    (subject/replace-with-indices [:a ALL :b (RANGE 1 100)] [17 3]) => [:a 17 :b 3]))
    
    
