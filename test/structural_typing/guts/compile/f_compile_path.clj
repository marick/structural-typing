(ns structural-typing.guts.compile.f-compile-path
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.compile.compile-path :as subject]
            [structural-typing.guts.preds.wrap :as wrap]
            [structural-typing.guts.exval :as exval]
            [structural-typing.guts.explanations :as explain]
            [com.rpl.specter :as specter])
  (:use midje.sweet
        structural-typing.assist.testutil
        structural-typing.assist.special-words))


;;;; The StructurePath extensions we use

(defn compile-and-run
  ([path input nil-and-missing-handling]
     (let [compiled (subject/compile-path path nil-and-missing-handling)]
       (subject/apply-path compiled input)))
  ([path input]
     (compile-and-run path input {})))

(defn err:compile-and-run
  ([path input nil-and-missing-handling]
     (for [result (compile-and-run path input nil-and-missing-handling)]
       (if-let [explainer (:explainer result)]
         (explainer result)
         (format "There is no explainer in %s" (into {} result)))))
  ([path input]
     (err:compile-and-run path input {})))

(fact "keywords"
  (fact "prints as keyword"
    (pr-str (subject/->KeywordPathElement :foo)) => ":foo")

  (fact "is created from keywords"
    (subject/path-element :a) => (subject/->KeywordPathElement :a))

  (fact "descends through a keyword"
    (compile-and-run [:a :b] {:a {:b 1}}) => (just {:path [:a :b]
                                                    :whole-value {:a {:b 1}}
                                                    :leaf-value 1}))
  (fact "rejects a non-associative structure"
    (err:compile-and-run [:a :b] {:a 1}) => (just (explain/err:shouldbe-maplike [:a :b] 1))
    (err:compile-and-run [:a :b] 2) => (just (explain/err:shouldbe-maplike [:a] 2)))

  (facts "about those troublesome special cases"
    (fact "normally tolerates being applied to `nil`"
      (compile-and-run [:a] nil) => (just {:path [:a]
                                           :whole-value nil
                                           :leaf-value nil}))

    (fact "but it can reject nil"
      (err:compile-and-run [:a :b] nil {:reject-nil? true})
      => (just (explain/err:should-not-be-applied-to-nil [:a])))

    (fact "it can reject a missing key"
      (err:compile-and-run [:a :b] {:a {}} {:reject-missing? true})
      => (just (explain/err:shouldbe-present [:a :b])))

    (fact "can be made to reject a nil-valued key"
      (err:compile-and-run [:a :b] {:a {:b nil}} {:reject-nil? true})
      => (just (explain/err:shouldbe-not-nil [:a :b] 1)))

    (fact "if both missing and nil are to be rejected, `missing` takes precedence"
      (err:compile-and-run [:a :b] {:a {}} {:reject-missing? true, :reject-nil? true})
      => (just (explain/err:shouldbe-present [:a :b])))))

(fact "strings"
  (fact "prints as string"
    (pr-str (subject/->StringPathElement "foo")) => "\"foo\"")

  (fact "is created from strings"
    (subject/path-element "a") => (subject/->StringPathElement "a"))

  ;; We happen to know that strings and keywords are handled by the same code,
  ;; so we don't test that much.
  (fact "descends through a string"
    (compile-and-run ["a" "b"] {"a" {"b" 1}}) => (just {:path ["a" "b"]
                                                        :whole-value {"a" {"b" 1}}
                                                        :leaf-value 1}))

  (fact "can be made to reject missing values"
    (err:compile-and-run ["a" "b"] {"a" {}} {:reject-missing? true})
    => (just (explain/err:shouldbe-present ["a" "b"]))))

(fact "ALL"
  (fact "prints as its name"
    (pr-str (subject/->AllPathElement)) => "ALL")

  (fact "is created from its name"
    (subject/path-element subject/ALL) => (subject/->AllPathElement))

  (fact "rejects a non-collection"
    (err:compile-and-run [:a subject/ALL] {:a "1"})
    => (just (explain/err:shouldbe-collection [:a subject/ALL] "1")))

  (fact "rejects a map"
    (err:compile-and-run [:a subject/ALL] {:a {:b 1}})
    => (just (explain/err:shouldbe-not-maplike [:a subject/ALL] {:b 1})))

  (fact "descends elements"
    (compile-and-run [subject/ALL] [:first :second]) => (just {:path [0]
                                                               :whole-value [:first :second]
                                                               :leaf-value :first}
                                                              {:path [1]
                                                               :whole-value [:first :second]
                                                               :leaf-value :second})
    (let [whole-value {:a [{:b 1}]}]
      (compile-and-run [:a subject/ALL :b] whole-value) => (just {:path [:a 0 :b]
                                                                  :whole-value whole-value
                                                                  :leaf-value 1}))

    (let [whole-value {:a [:key 1]}]
      (err:compile-and-run [:a subject/ALL :b] whole-value)
      => (just (explain/err:shouldbe-maplike [:a 0 :b] :key)
               (explain/err:shouldbe-maplike [:a 1 :b] 1))))

  (facts "about those troublesome special cases"
    (fact "normally tolerates being applied to `nil`"
      (compile-and-run [subject/ALL] nil) => empty?)

    (fact "but it can reject nil"
      (err:compile-and-run [subject/ALL] nil {:reject-nil? true})
      => (just (explain/err:should-not-be-applied-to-nil [subject/ALL])))

    (fact "reject-missing? has no meaning for subject/ALL"
      (compile-and-run [subject/ALL] [] {:reject-missing? true}) => [])

    (fact "normally happy to descend into a nil value"
      (compile-and-run [subject/ALL] [nil]) => (just {:path [0]
                                              :whole-value [nil]
                                              :leaf-value nil}))

    (fact "but can be made to reject descending into a nil value"
      (err:compile-and-run [subject/ALL] [nil] {:reject-nil? true})
      => (just (explain/err:shouldbe-not-nil [0])))

    (fact "note that one nil value does not prevent handling of valid values"
      (let [result (compile-and-run [subject/ALL] [nil 3] {:reject-nil? true})]
        (first result) => (contains {:explainer anything
                                     :path [0]
                                     :whole-value [nil 3]
                                     :leaf-value nil})
        ((:explainer (first result)) (first result)) => (just (explain/err:shouldbe-not-nil [0]))
        (second result) => (just {:path [1]
                                  :whole-value [nil 3]
                                  :leaf-value 3})))))


(fact "RANGE"
  (let [r1-3 (subject/RANGE 1 3)]
    (fact "prints as its definition"
      (pr-str (subject/->RangePathElement 1 2 [1 2] 1)) => "(RANGE 1 2)")

    (fact "is created from its name"
      (subject/path-element (subject/RANGE 1 2)) => (subject/->RangePathElement 1 2 [1 2] 1))

    (fact "rejects impossible cases"
      (subject/RANGE 2 2) => (throws "Second argument of `(RANGE 2 2)` should be greater than the first")
      (subject/RANGE -1 2) => (throws "`(RANGE -1 2)` has a negative lower bound")
      (subject/RANGE :a 2) => (throws "Every argument to `(RANGE :a 2)` should be an integer")
      (subject/RANGE 1 :b) => (throws "Every argument to `(RANGE 1 :b)` should be an integer"))

    (fact "rejects non-sequential collections"
      (err:compile-and-run [:a r1-3] {:a "1"})
      => (just (explain/err:shouldbe-sequential [:a r1-3] "1"))
      (err:compile-and-run [:a r1-3] {:a #{"1"}})
      => (just (explain/err:shouldbe-sequential [:a r1-3] #{"1"})))

    (fact "descends elements"
      (let [whole-value [:skipped :one :two :skipped]]
        (compile-and-run [r1-3] whole-value) => (just {:path [1]
                                                       :whole-value whole-value
                                                       :leaf-value :one}
                                                      {:path [2]
                                                       :whole-value whole-value
                                                       :leaf-value :two}))

      (let [whole-value {:a [:skipped :key 1 :skipped]}]
        (err:compile-and-run [:a r1-3 :b] whole-value)
        => (just (explain/err:shouldbe-maplike [:a 1 :b] :key)
                 (explain/err:shouldbe-maplike [:a 2 :b] 1))))

    (fact "works with various types, not just arrays"
      (compile-and-run [(subject/RANGE 1 2) :b] '({:b :first} {:b :second}))
      => (just {:path [1 :b]
                :whole-value '({:b :first} {:b :second})
                :leaf-value :second})
      (let [[result & _ :as all] (compile-and-run [(subject/RANGE 1000 1001)] (range))]
        ;; separate checks are so that printing failure messages doesn't go infinite
        (count all) => 1
        (:path result) => [1000]
        (:leaf-value result) => 1000))

    (facts "about those troublesome special cases"
      (fact "applying the range to `nil`"
        (fact "normally tolerates being applied to `nil`"
          (compile-and-run [r1-3] nil) => (just {:path [1]
                                                 :whole-value nil
                                                 :leaf-value nil}
                                                {:path [2]
                                                 :whole-value nil
                                                 :leaf-value nil}))

        (fact "but it can reject nil"
          (err:compile-and-run [r1-3] nil {:reject-nil? true})
          => (just (explain/err:should-not-be-applied-to-nil [r1-3])))

        (fact "when reject-missing is given, you get a separate oopsie for each missing value"
          (err:compile-and-run [r1-3] nil {:reject-missing? true})
          => (just (explain/err:shouldbe-present [1])
                   (explain/err:shouldbe-present [2])))

        (fact "when both are given, the `reject-nil` takes precedence"
          (err:compile-and-run [r1-3] nil {:reject-nil? true :reject-missing? true})
          => (just (explain/err:should-not-be-applied-to-nil [r1-3]))))

      (facts "about missing values - ones beyond the size of the sequence"
        (let [whole-value [:skipped :present]]
          (fact "normally happy to generate nil values"
            (compile-and-run [r1-3] whole-value)
            => (just {:path [1]
                      :whole-value whole-value
                      :leaf-value :present}
                     {:path [2]
                      :whole-value whole-value
                      :leaf-value nil}))

          (fact "reject-missing? produces errors instead"
            (let [result (compile-and-run [r1-3] whole-value {:reject-missing? true})]
              (first result) => {:path [1]
                                 :whole-value whole-value
                                 :leaf-value :present}
              (second result) => (contains {:explainer anything
                                            :path [2]})
              ((:explainer (second result)) (second result))
              => (just (explain/err:shouldbe-present [2]))))))

      (facts "about nil values"
        (let [whole-value [:skipped :ok nil :skipped]]
          (fact "normally happy to descend into a nil value"
            (compile-and-run [r1-3] whole-value) => (just {:path [1]
                                                           :whole-value whole-value
                                                           :leaf-value :ok}
                                                          {:path [2]
                                                           :whole-value whole-value
                                                           :leaf-value nil}))

          (fact "but can be made to reject descending into a nil value"
            (let [result (compile-and-run [r1-3] whole-value {:reject-nil? true})]
              (first result) => {:path [1]
                                 :whole-value whole-value
                                 :leaf-value :ok}
              (second result) => (contains {:explainer anything
                                            :path [2]})
              ((:explainer (second result)) (second result))
              => (just (explain/err:shouldbe-not-nil [2]))))

          (fact "in a sort of odd twist, you can reject nils that got appended"
            (let [path [:a (subject/RANGE 0 3) :b]
                  whole-value {:a [nil {:b nil}]}
                  result (err:compile-and-run path whole-value {:reject-nil? true})]
              result => (just (explain/err:shouldbe-not-nil [:a 0])
                              (explain/err:shouldbe-not-nil [:a 1 :b])
                              (explain/err:shouldbe-not-nil [:a 2]))))

          (fact "reject-missing takes precedence"
            (let [path [:a (subject/RANGE 0 3) :b]
                  whole-value {:a [nil {:b nil}]}
                  result (err:compile-and-run path whole-value {:reject-nil? true :reject-missing? true})]
              result => (just (explain/err:shouldbe-not-nil [:a 0])
                              (explain/err:shouldbe-not-nil [:a 1 :b])
                              (explain/err:shouldbe-present [:a 2])))))))))



(fact "integers"
  (fact "prints as the integer"
    (pr-str (subject/->IntegerPathElement 3)) => "3")

  (fact "is created from an integer"
    (subject/path-element 1) => (subject/->IntegerPathElement 1))

  (fact "rejects non-sequential collections"
    (err:compile-and-run [:a 0] {:a "1"}) => (just (explain/err:shouldbe-sequential [:a 0] "1"))
    (err:compile-and-run [:a 0] {:a #{"1"}}) => (just (explain/err:shouldbe-sequential [:a 0] #{"1"})))

  (fact "descends elements"
    (compile-and-run [1] [:first :second]) => (just {:path [1]
                                                     :whole-value [:first :second]
                                                     :leaf-value :second})
    (let [whole-value {:a [{:b 1}]}]
      (compile-and-run [:a 0 :b] whole-value) => (just {:path [:a 0 :b]
                                                        :whole-value whole-value
                                                        :leaf-value 1}))

    (let [whole-value {:a [:key 1]}]
      (err:compile-and-run [:a 1 :b] whole-value)
      => (just (explain/err:shouldbe-maplike [:a 1 :b] 1))))

  (future-fact "works with various types"
    (compile-and-run [1] '(:first :second)) => (just {:path [1]
                                                      :whole-value [:first :second]
                                                      :leaf-value :second})
    (compile-and-run [1000] (range)) => (just (contains {:path [1]
                                                         :leaf-value 1000})))

  (facts "about those troublesome special cases"
    (fact "normally tolerates being applied to `nil`"
      (compile-and-run [0] nil) => nil
      (compile-and-run [1] nil) => nil)

    (fact "but it can reject nil"
      (err:compile-and-run [0] nil {:reject-nil? true})
      => (just (explain/err:should-not-be-applied-to-nil [0])))

    (future-fact "reject-missing? works for types with and without an integer key"
      (compile-and-run [0] [] {:reject-missing? true})
      => (just (explain/err:shouldbe-present [0]))
      (compile-and-run [333] [0 1] {:reject-missing? true})
      => (just (explain/err:shouldbe-present [333]))
      (compile-and-run [333] '(0 1) {:reject-missing? true})
      => (just (explain/err:shouldbe-present [333])))

    (fact "normally happy to descend into a nil value"
      (compile-and-run [:a 1] {:a [0 nil]}) => (just {:path [:a 1]
                                                      :whole-value {:a [0 nil]}
                                                      :leaf-value nil}))

    (future-fact "but can be made to reject descending into a nil value"
      (err:compile-and-run [subject/ALL] [nil] {:reject-nil? true})
      => (just (explain/err:shouldbe-not-nil [0])))

    (future-fact "note that one nil value does not prevent handling of valid values"
      (let [result (compile-and-run [subject/ALL] [nil 3] {:reject-nil? true})]
        (first result) => (contains {:explainer anything
                                     :path [0]
                                     :whole-value [nil 3]
                                     :leaf-value nil})
        ((:explainer (first result)) (first result)) => (just (explain/err:shouldbe-not-nil [0]))
        (second result) => (just {:path [1]
                                  :whole-value [nil 3]
                                  :leaf-value 3})))))
