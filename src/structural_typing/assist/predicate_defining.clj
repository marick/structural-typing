(ns structural-typing.assist.predicate-defining
  "Help for defining custom predicates"
  (:use structural-typing.clojure.core)
  (:require [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.guts.preds.annotating :as annotating]
            [such.readable :as readable]))

(defn should-be [format-string expected]
  #(format format-string,
           (oopsie/friendly-path %)
           (readable/value-string expected)
           (readable/value-string (:leaf-value %))))

(defn compose-predicate [name pred fmt-fn]
  (->> pred
       (annotating/show-as name)
       (annotating/explain-with fmt-fn)))

(def exactly-format "%s should be exactly `%s`; it is `%s`")

(defn exactly
  "A predicate that succeeds exactly when its argument is `=` to expected.

        (built-like? even? even?) ;=> false
        (built-like? (exactly even?) even?) ;=> true

  Note: except for vectors, maps, and functions, `exactly` is added implicitly:

        (built-like {[ALL :a] 1} [{:a 1} {:a 2}]) ;=> [1 :a] should be exactly `1`; it is `2`
" 
  [expected]
  (compose-predicate
   (format "(exactly %s)" (readable/value-string expected))
   (partial = expected)
   (should-be exactly-format expected)))

(defn regex-match [expected]
  (compose-predicate
   (format "(regex-match %s)" (str expected))
   (fn [actual]
     (branch-on actual
       string?   (boolean (re-find expected actual))
       regex?    (= (str expected) (str actual))
       :else     false))
   (fn [oopsie]
     (let [actual (:leaf-value oopsie)
           readable-actual (readable/value-string actual)]

       (format "%s should match %s; it is %s"
               (oopsie/friendly-path oopsie)
               (readable/value-string expected)
               (if (or (string? actual) (regex? actual))
                 readable-actual
                 (str "`" readable-actual "`")))))))


(defn- bigfloatlike? [n]
  (or (= (type n) (type 1N))
      (= (type n) (type 1M))))

(defn number-match [expected]
  (let [[comparison format-string] (if (bigfloatlike? expected)
                                      [== "%s should be `==` to %s; it is %s"]
                                      [= exactly-format])]
    (compose-predicate
     (format "(number-match %s)" expected)
     (partial comparison expected)
     (should-be format-string expected))))

(defn pretty-record-class [r]
  (-> (type r)
      pr-str
      (str-split #"\.")
      last))

(defn pretty-record-instance [r]
  (str "#" (pretty-record-class r) (pr-str (into {} r))))

(defn record-match [expected]
  (compose-predicate
   (format "(record-match %s)" (pretty-record-instance expected))
   (partial = expected)
   (fn [{actual :leaf-value :as oopsie}]
     (let [path (oopsie/friendly-path oopsie)]
       (cond (classic-map? actual)
             (format "%s should be a record; it is plain map `%s`" path actual)

             (not= (type expected) (type actual))
             (format "%s should be a `%s` record; it is %s `%s`"
                     path
                     (pretty-record-class expected)
                     (pretty-record-class actual)
                     (readable/value-string actual))

             :else
             (format exactly-format
                     path
                     (pretty-record-instance expected)
                     (pretty-record-instance actual)))))))
