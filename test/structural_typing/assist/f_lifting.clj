(ns structural-typing.assist.f-lifting
  (:require [structural-typing.assist.lifting :as subject]
            [such.readable :as readable]
            [structural-typing.guts.expred :as expred]
            [structural-typing.guts.type-descriptions.elements :refer [ALL]]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.guts.type-descriptions.substituting :as substituting]
            [structural-typing.guts.preds.wrap :as wrap])
  (:use midje.sweet structural-typing.assist.testutil))

(fact "lifted predicates are given the value to test in a map and return oopsies"
  (let [lifted (subject/lift-pred even?)]
    (lifted {:leaf-value 3}) => (just (contains {:leaf-value 3}))
    (lifted {:leaf-value 4}) => empty?

    (fact "information about the predicate is contained in the failed oopsie"
      (lifted {:leaf-value 3}) => (just (contains {:predicate (exactly even?)})))

    (fact "any information passed in is retained in the output"
      (lifted {:leaf-value 3 :anything "here"}) => (just (contains {:anything "here"})))))

(facts "The oopsie contains gives the information needed to produce an error string"
  (fact "a named function is shown in a friendly way"
    (lift-and-run even? (exval 3)) => (just (contains {:predicate-string "even?"
                                                       :leaf-value 3
                                                       :explainer expred/default-predicate-explainer})))

  (fact "Lifting depends on clever predicate name extraction"
    (let [any-old-predicate (constantly false)]
      (lift-and-run any-old-predicate (exval 3)) => (just (contains {:predicate-string ..name..}))
      (provided
        (wrap/get-predicate-string any-old-predicate) => ..name..))))
  
(fact "lifting a var is like lifting a function"
  (lift-and-run #'even? (exval 3))
  => (just (contains {:predicate #'even?
                      :predicate-string "even?"
                      :explainer expred/default-predicate-explainer})))

(fact "lifting normally converts nil values to success"
  ( (subject/lift-pred (complement nil?)) {:leaf-value nil}) => empty?
  ( (subject/lift-pred (complement nil?) :check-nils) {:leaf-value nil}) =not=> empty?)

(fact "lifting normally converts exceptions into an oopsie"
  (even? 'derp) => (throws)
  ( (subject/lift-pred even?) {:leaf-value 'derp}) =not=> empty? 
  ( (subject/lift-pred even? :allow-exceptions) {:leaf-value 'derp}) => (throws))

    

(fact "an already-lifted predicate is left unchanged"
  (let [once (subject/lift-pred even?)
        twice (subject/lift-pred once)]
    (identical? once twice) => true)

  (fact "note that means that additions cannot be changed"
    (let [once (subject/lift-pred even?)
          twice (subject/lift-pred once :allow-exceptions)]
      (identical? once twice) => true)))



(fact "lifting type-descriptions into an oopsie-producing predicate"
  (let [r (subject/lift-type [even?])]
    (r 2) => empty?
    (oopsie/explanations (r 1)) => (just "Value should be `even?`; it is `1`"))

  (let [r (subject/lift-type [ [:a :b] {:c even?}])]
    (r {:a 1, :b 2}) => empty?
    (oopsie/explanations (r {:c 1, :b 2})) => (just ":a must exist and be non-nil"
                                                    ":c should be `even?`; it is `1`"))

  (fact "type maps can be expanded"
    (let [r (subject/lift-type [ [:a :b] (substituting/includes :X)]
                               {:X {:x even?}})]
      (r {:a 1, :b 2}) => empty?
      (r {:a 1, :b 2, :x 2}) => empty?
      (oopsie/explanations (r {:b 2, :x 1})) => (just ":a must exist and be non-nil"
                                                      ":x should be `even?`; it is `1`"))))
    
                                                    
