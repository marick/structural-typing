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


  (fact "can be made to reject missing values"
    (err:compile-and-run [:a :b] {:a {}} {:reject-missing? true})
    => (just (explain/err:missing [:a :b])))
  (fact "can be made to reject nil values"
    (err:compile-and-run [:a :b] {:a {:b nil}} {:reject-nil? true})
    => (just (explain/err:nil [:a :b] 1)))

  (fact "missing takes precedence"
    (err:compile-and-run [:a :b] {:a {}} {:reject-missing? true, :reject-nil? true})
    => (just (explain/err:missing [:a :b]))))

(fact "strings"
  (fact "prints as string"
    (pr-str (subject/->StringPathElement "foo")) => "\"foo\"")

  (fact "is created from strings"
    (subject/path-element "a") => (subject/->StringPathElement "a"))

  (fact "descends through a string"
    (compile-and-run ["a" "b"] {"a" {"b" 1}}) => (just {:path ["a" "b"]
                                                        :whole-value {"a" {"b" 1}}
                                                        :leaf-value 1}))
  (fact "rejects a non-associative structure"
    (err:compile-and-run ["a" "b"] {"a" 1}) => (just (explain/err:shouldbe-maplike ["a" "b"] 1))
    (err:compile-and-run ["a" "b"] 2) => (just (explain/err:shouldbe-maplike ["a"] 2)))


  (fact "can be made to reject missing values"
    (err:compile-and-run ["a" "b"] {"a" {}} {:reject-missing? true})
    => (just (explain/err:missing ["a" "b"])))
  (fact "can be made to reject nil values"
    (err:compile-and-run ["a" "b"] {"a" {"b" nil}} {:reject-nil? true})
    => (just (explain/err:nil ["a" "b"] 1)))
  (fact "missing takes precedence"
    (err:compile-and-run [:a :b] {:a {}} {:reject-missing? true, :reject-nil? true})
    => (just (explain/err:missing [:a :b]))))

(fact "ALL"
  (fact "prints as its name"
    (pr-str (subject/->AllPathElement)) => "ALL")

  (fact "is created from its name"
    (subject/path-element subject/ALL) => (subject/->AllPathElement))

  (fact "rejects a non-collection"
    (err:compile-and-run [:a ALL] {:a "1"}) => (just (explain/err:shouldbe-collection [:a ALL] "1")))

  (fact "rejects a map"
    (err:compile-and-run [:a ALL] {:a {:b 1}})
    => (just (explain/err:shouldbe-not-maplike [:a ALL] {:b 1})))

  (fact "descends elements"
    (compile-and-run [ALL] [:first :second]) => (just {:path [0]
                                                       :whole-value [:first :second]
                                                       :leaf-value :first}
                                                      {:path [1]
                                                       :whole-value [:first :second]
                                                       :leaf-value :second})
    (let [whole-value {:a [{:b 1}]}]
      (compile-and-run [:a ALL :b] whole-value) => (just {:path [:a 0 :b]
                                                          :whole-value whole-value
                                                          :leaf-value 1}))

    (let [whole-value {:a [:key 1]}]
      (err:compile-and-run [:a ALL :b] whole-value)
      => (just (explain/err:shouldbe-maplike [:a 0 :b] :key)
               (explain/err:shouldbe-maplike [:a 1 :b] 1))))

  (fact "nil is by default treated as an empty array"
    (let [whole-value {:a nil}]
      (compile-and-run [:a ALL] whole-value) => empty?))

  (fact "both :reject-missing? and :reject-nil? apply to nil values"
    (err:compile-and-run [:a ALL] {:a nil} {:reject-missing? true})
    => (just (explain/err:all-missing [:a ALL])))
  )
