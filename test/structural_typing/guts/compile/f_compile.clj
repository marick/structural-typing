(ns structural-typing.guts.compile.f-compile
  (:require [com.rpl.specter :as specter])
  (:require [structural-typing.guts.compile.compile :as subject]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.guts.compile.compile-path :as path]
            [structural-typing.guts.preds.wrap :as wrap]
            [structural-typing.guts.preds.pseudopreds :as pseudo])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))



;;;;;;;;;; OLD OLD OLD

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

    
    
;;;;;; NEW

(fact "the interaction between the path and preds is a bit subtle"
  (subject/compile-pair [..path.. [even? required-path]])
  => [..compiled-paths.. ..compiled-preds..]
  (provided
    ; Note that the pseudopredicates are stripped out.
    (subject/compile-predicates [even?]) => ..compiled-preds..
    ; ... which affects how the path is compiled.
    (path/compile-path ..path.. {:reject-nil? true, :reject-missing? true}) => ..compiled-paths..))

;;; How compiled path/pred pairs are used
(fact "the pred runs over one or more values"
  (oopsie/explanations (subject/run-compiled-pairs {:a 1}
                                                   [[(path/compile-path [:a] {})
                                                     (subject/compile-predicates [even?])]]))
  => (just (err:shouldbe :a "even?" 1))

  (oopsie/explanations (subject/run-compiled-pairs {:a 2}
                                                   [[(path/compile-path [:a] {})
                                                     (subject/compile-predicates [even?])]]))
  => empty?

  (oopsie/explanations (subject/run-compiled-pairs {:a 1}
                                                   [[(path/compile-path [:a] {})
                                                     (subject/compile-predicates [even?])]
                                                    [(path/compile-path [:a] {})
                                                     (subject/compile-predicates [neg? string?])]]))
  => (just (err:shouldbe :a "even?" 1)
           (err:shouldbe :a "neg?" 1)
           (err:shouldbe :a "string?" 1)))
(fact "compiling a whole canonicalized type"
  (let [compiled (subject/compile-type-2 {[:a] [even?]})]
    (oopsie/explanations (compiled {:a 1})) => (just (err:shouldbe :a "even?" 1)))

  (let [compiled (subject/compile-type-2 {[:a] [even?]
                                          [:b] [required-path even?]})]
    (oopsie/explanations (compiled {})) => (just (err:shouldbe-present :b))
    (oopsie/explanations (compiled {:a 1, :b 3})) => (just (err:shouldbe :a "even?" 1)
                                                           (err:shouldbe :b "even?" 3))
    (oopsie/explanations (compiled {:a 4, :b 2})) => empty?)


  (let [compiled (subject/compile-type-2 {[:a path/ALL :b] [even?]
                                          [:a path/ALL] [required-path]})]

    (oopsie/explanations (compiled {:a 5})) => (just (err:shouldbe-collection [:a path/ALL] 5))
    (oopsie/explanations (compiled {:a []})) => empty?
    (oopsie/explanations (compiled {:a [{}]})) => empty?
    (oopsie/explanations (compiled {:a [{:b nil}]})) => empty?
    (oopsie/explanations (compiled {:a [{:b 4}]})) => empty?
    (oopsie/explanations (compiled {:a [{:b 555}]})) => (just (err:shouldbe [:a 0 :b] "even?" 555))

)


  )





;; (fact
;;   (let [compiled (subject/
;;   (oopsie/explanations (subject

  ;; (prn :=================)
  ;; (prn ( (subject/compile-predicates [peven?]) ))
  ;; (let [f (subject/mkfn:whole-value->oopsies )]
  ;;   (f {:a 2}) => empty?
  ;;   (oopsie/explanations (f {:a 1})) => 33
  ;; ))
