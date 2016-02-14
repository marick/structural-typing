(ns structural-typing.assist.format
  "I'm trying to figure out a good way to decompose describing bad values. This is a
   partial start. You should ignore it."
  (:use structural-typing.clojure.core)
  (:require [such.readable :as readable]))

(def anonymous-name "<custom-predicate>")
(readable/set-function-elaborations! {:anonymous-name anonymous-name :surroundings ""})


(defn leaf:fn [f]
  (let [s (readable/value-string f)]
    (if (= s anonymous-name)
      (pr-str f)
      s)))


(defn leaf [leaf-value]
  (cond (extended-fn? leaf-value)
        (format "the function `%s`" (leaf:fn leaf-value))

        :else
        (str "`" (pr-str leaf-value) "`")))


(defn record-class [r]
  (-> (type r)
      pr-str
      (str-split #"\.")
      last))

(defn leaf:record [r]
  (str "#" (record-class r) (pr-str (into {} r))))
