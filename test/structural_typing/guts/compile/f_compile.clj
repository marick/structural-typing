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
  (fact "simplest example"
    (let [compiled (subject/compile-type {[:a] [even?]})]
      (oopsie/explanations (compiled {:a 1})) => (just (err:shouldbe :a "even?" 1))))

  (fact "ultiple keys can produce messages"
    (let [compiled (subject/compile-type {[:a] [even?]
                                          [:b] [required-path even?]})]
      (oopsie/explanations (compiled {})) => (just (err:missing :b))
      (oopsie/explanations (compiled {:a 1, :b 3})) => (just (err:shouldbe :a "even?" 1)
                                                             (err:shouldbe :b "even?" 3))
      (oopsie/explanations (compiled {:a 4, :b 2})) => empty?))


  (fact "an example of ALL"
    (let [compiled (subject/compile-type {[:a path/ALL :b] [even?]
                                          [:a path/ALL] [required-path]})]

      (oopsie/explanations (compiled {:a []})) => empty?
      (oopsie/explanations (compiled {:a [{}]})) => empty?
      (oopsie/explanations (compiled {:a [{:b nil}]})) => empty?
      (oopsie/explanations (compiled {:a [{:b 4}]})) => empty?
      (oopsie/explanations (compiled {:a [{:b 555}]})) => (just (err:shouldbe [:a 0 :b] "even?" 555))))

  (fact "if ALL paths share a prefix, there's only one error message"
    ;; Currently, you get two messages: one for each of the two paths below
    (let [compiled (subject/compile-type {[:a path/ALL :b] [even?]
                                          [:a path/ALL] [required-path]})]

      (oopsie/explanations (compiled {:a 5})) => (just (err:not-collection [:a path/ALL] 5))))

  (fact "oopsies due to bad structures are not sent for further processing"
    (let [compiled (subject/compile-type {[path/ALL] [even?]})]
      (oopsie/explanations (compiled 2)) => (just (err:not-collection [path/ALL] 2)))))
