(ns structural-typing.assist.format
  (:use structural-typing.clojure.core)
  (:require [such.readable :as readable]))

(def anonymous-name "<custom-predicate>")
(readable/set-function-elaborations! {:anonymous-name anonymous-name :surroundings ""})


(defn function-as-bad-value-string [f]
  (let [s (readable/value-string f)]
    (if (= s anonymous-name)
      (pr-str f)
      s)))


(defn explain-leaf-value [leaf-value]
  (cond (extended-fn? leaf-value)
        (format "the function `%s`" (function-as-bad-value-string leaf-value))

        :else
        (str "`" (pr-str leaf-value) "`")))


(defn pretty-record-class [r]
  (-> (type r)
      pr-str
      (str-split #"\.")
      last))

(defn pretty-record-instance [r]
  (str "#" (pretty-record-class r) (pr-str (into {} r))))
