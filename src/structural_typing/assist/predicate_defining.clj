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

(defn exactly [expected]
  (compose-predicate
   (format "(exactly %s)" (readable/value-string expected))
   (partial = expected)
   (should-be "%s should be exactly `%s`; it is `%s`" expected)))

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
