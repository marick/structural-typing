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
    (err:compile-and-run [:a :b] {:a 1}) => (just (explain/err:not-maplike [:a :b] 1))
    (err:compile-and-run [:a :b] 2) => (just (explain/err:not-maplike [:a] 2)))

  (facts "about those troublesome special cases"
    (fact "normally tolerates being applied to `nil`"
      (compile-and-run [:a] nil) => (just {:path [:a]
                                           :whole-value nil
                                           :leaf-value nil}))

    (fact "but it can reject nil"
      (err:compile-and-run [:a :b] nil {:reject-nil? true})
      => (just (explain/err:selector-at-nil [:a])))

    (fact "it can reject a missing key"
      (err:compile-and-run [:a :b] {:a {}} {:reject-missing? true})
      => (just (explain/err:missing [:a :b])))

    (fact "can be made to reject a nil-valued key"
      (err:compile-and-run [:a :b] {:a {:b nil}} {:reject-nil? true})
      => (just (explain/err:value-nil [:a :b])))

    (fact "if both missing and nil are to be rejected, `missing` takes precedence"
      (err:compile-and-run [:a :b] {:a {}} {:reject-missing? true, :reject-nil? true})
      => (just (explain/err:missing [:a :b])))))

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
    => (just (explain/err:missing ["a" "b"]))))

(fact "ALL"
  (fact "prints as its name"
    (pr-str (subject/->AllPathElement)) => "ALL")

  (fact "is created from its name"
    (subject/path-element subject/ALL) => (subject/->AllPathElement))

  (fact "rejects a non-collection"
    (err:compile-and-run [:a subject/ALL] {:a "1"})
    => (just (explain/err:not-collection [:a subject/ALL] "1")))

  (fact "rejects a map"
    (err:compile-and-run [:a subject/ALL] {:a {:b 1}})
    => (just (explain/err:maplike [:a subject/ALL] {:b 1})))

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
      => (just (explain/err:not-maplike [:a 0 :b] :key)
               (explain/err:not-maplike [:a 1 :b] 1))))

  (facts "about those troublesome special cases"
    (fact "normally tolerates being applied to `nil`"
      (compile-and-run [subject/ALL] nil) => empty?)

    (fact "but it can reject nil"
      (err:compile-and-run [subject/ALL] nil {:reject-nil? true})
      => (just (explain/err:selector-at-nil [subject/ALL])))

    (fact "reject-missing? has no meaning for subject/ALL"
      (compile-and-run [subject/ALL] [] {:reject-missing? true}) => [])

    (fact "normally happy to descend into a nil value"
      (compile-and-run [subject/ALL] [nil]) => (just {:path [0]
                                              :whole-value [nil]
                                              :leaf-value nil}))

    (fact "but can be made to reject descending into a nil value"
      (err:compile-and-run [subject/ALL] [nil] {:reject-nil? true})
      => (just (explain/err:value-nil [0])))

    (fact "note that one nil value does not prevent handling of valid values"
      (let [result (compile-and-run [subject/ALL] [nil 3] {:reject-nil? true})]
        (first result) => (contains {:explainer anything
                                     :path [0]
                                     :whole-value [nil 3]
                                     :leaf-value nil})
        ((:explainer (first result)) (first result)) => (just (explain/err:value-nil [0]))
        (second result) => (just {:path [1]
                                  :whole-value [nil 3]
                                  :leaf-value 3})))))


(fact "RANGE"
  (let [r1-3 (subject/RANGE 1 3)]
    (fact "prints as its definition"
      (pr-str (subject/RANGE 1 2)) => "(RANGE 1 2)")

    (fact "rejects impossible cases"
      (subject/RANGE 2 2) => (throws "Second argument of `(RANGE 2 2)` should be greater than the first")
      (subject/RANGE -1 2) => (throws "`(RANGE -1 2)` has a negative lower bound")
      (subject/RANGE :a 2) => (throws "Every argument to `(RANGE :a 2)` should be an integer")
      (subject/RANGE 1 :b) => (throws "Every argument to `(RANGE 1 :b)` should be an integer"))

    (fact "rejects non-sequential collections"
      (err:compile-and-run [:a r1-3] {:a "1"})
      => (just (explain/err:not-sequential [:a r1-3] "1"))
      (err:compile-and-run [:a r1-3] {:a #{"1"}})
      => (just (explain/err:not-sequential [:a r1-3] #{"1"})))

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
        => (just (explain/err:not-maplike [:a 1 :b] :key)
                 (explain/err:not-maplike [:a 2 :b] 1))))

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
          => (just (explain/err:selector-at-nil [r1-3])))

        (fact "when reject-missing is given, you get a separate oopsie for each missing value"
          (err:compile-and-run [r1-3] nil {:reject-missing? true})
          => (just (explain/err:missing [1])
                   (explain/err:missing [2])))

        (fact "when both are given, the `reject-nil` takes precedence"
          (err:compile-and-run [r1-3] nil {:reject-nil? true :reject-missing? true})
          => (just (explain/err:selector-at-nil [r1-3]))))

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
              => (just (explain/err:missing [2]))))))

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
              => (just (explain/err:value-nil [2]))))

          (fact "in a sort of odd twist, you can reject nils that got appended"
            (let [path [:a (subject/RANGE 0 3) :b]
                  whole-value {:a [nil {:b nil}]}
                  result (err:compile-and-run path whole-value {:reject-nil? true})]
              result => (just (explain/err:value-nil [:a 0])
                              (explain/err:value-nil [:a 1 :b])
                              (explain/err:value-nil [:a 2]))))

          (fact "reject-missing takes precedence"
            (let [path [:a (subject/RANGE 0 3) :b]
                  whole-value {:a [nil {:b nil}]}
                  result (err:compile-and-run path whole-value {:reject-nil? true :reject-missing? true})]
              result => (just (explain/err:value-nil [:a 0])
                              (explain/err:value-nil [:a 1 :b])
                              (explain/err:missing [:a 2])))))))))


(fact "integers"
  (fact "prints as the integer"
    (pr-str (subject/path-element 3)) => "3")

  (fact "rejects non-sequential collections"
    (err:compile-and-run [:a 1] {:a "1"})
    => (just (explain/err:not-sequential [:a 1] "1"))
    (err:compile-and-run [:a 1] {:a #{"1"}})
    => (just (explain/err:not-sequential [:a 1] #{"1"})))

    (fact "descends elements"
      (let [whole-value [:skipped :one :two :skipped]]
        (compile-and-run [1] whole-value) => (just {:path [1]
                                                       :whole-value whole-value
                                                       :leaf-value :one}))

      (let [whole-value {:a [:skipped :key 1 :skipped]}]
        (err:compile-and-run [:a 1 :b] whole-value)
        => (just (explain/err:not-maplike [:a 1 :b] :key))))

  (fact "works with various types, not just arrays"
      (compile-and-run [1 :b] '({:b :first} {:b :second}))
      => (just {:path [1 :b]
                :whole-value '({:b :first} {:b :second})
                :leaf-value :second})
      (let [[result & _ :as all] (compile-and-run [1000] (map (partial * 2) (range)))]
        ;; separate checks are so that printing failure messages doesn't go infinite
        (count all) => 1
        (:path result) => [1000]
        (:leaf-value result) => 2000))

    (facts "about those troublesome special cases"
      (fact "applying the integer to `nil`"
        (fact "normally tolerates being applied to `nil`"
          (compile-and-run [1] nil) => (just {:path [1]
                                              :whole-value nil
                                              :leaf-value nil}))

        (fact "but it can reject nil"
          (err:compile-and-run [1] nil {:reject-nil? true})
          => (just (explain/err:selector-at-nil [1])))

        (fact "when reject-missing is given, you get a different message"
          (err:compile-and-run [1] nil {:reject-missing? true})
          => (just (explain/err:missing [1])))

        (fact "when both are given, the `reject-nil` takes precedence"
          (err:compile-and-run [1] nil {:reject-nil? true :reject-missing? true})
          => (just (explain/err:selector-at-nil [1]))))

      (facts "about missing values - ones beyond the size of the sequence"
        (let [whole-value [:skipped :present]]
          (fact "normally happy to generate a nil value"
            (compile-and-run [5] whole-value)
            => (just {:path [5]
                      :whole-value whole-value
                      :leaf-value nil}))

          (fact "reject-missing? produces errors instead"
            (let [result (compile-and-run [500] whole-value {:reject-missing? true})]
              (first result) => (contains {:explainer anything
                                           :path [500]})
              ((:explainer (first result)) (first result))
              => (just (explain/err:missing [500]))))))

      (facts "about nil values"
        (let [whole-value [:skipped :ok nil :skipped]]
          (fact "normally happy to descend into a nil value"
            (compile-and-run [2] whole-value) => (just {:path [2]
                                                        :whole-value whole-value
                                                        :leaf-value nil}))

          (fact "but can be made to reject descending into a nil value"
            (let [result (compile-and-run [2] whole-value {:reject-nil? true})]
              (first result) => (contains {:explainer anything
                                           :path [2]})
              ((:explainer (first result)) (first result))
              => (just (explain/err:value-nil [2]))))

          (fact "reject-missing takes precedence"
            (let [whole-value {:a [nil {:b nil}]}
                  rejections {:reject-nil? true :reject-missing? true}
                  pick (fn [idx] (err:compile-and-run [:a idx :b] whole-value rejections))]
              (pick 0) => (just (explain/err:value-nil [:a 0]))
              (pick 1) => (just (explain/err:value-nil [:a 1 :b]))
              (pick 2) => (just (explain/err:missing [:a 2]))))))))


(future-fact "the empty input is a special case")
