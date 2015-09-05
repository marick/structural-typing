(ns structural-typing.assist.testutil
  (:require [structural-typing.guts.exval :as exval]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.assist.lifting :as lifting]
            [structural-typing.type :as type]
            [such.readable :as readable]
            [clojure.string :as str])
  (:use [such.imperfection :only [val-and-output]]))


(defn exval
  ([leaf-value path whole-value]
     (exval/boa leaf-value path whole-value))
  ([leaf-value path]
     (exval leaf-value path (hash-map path leaf-value)))
  ([leaf-value]
     (exval leaf-value [:x])))

(defn explain-lifted
  "Note that it's safe to use this on an already-lifted predicate"
  [pred exval]
  (oopsie/explanations ((lifting/lift pred) exval)))

;; Don't use Midje checkers to avoid dragging in all of its dependencies

(defn oopsie-for [leaf-value & {:as kvs}]
  (let [expected (assoc kvs :leaf-value leaf-value)]
    (fn [actual]
      (= (select-keys actual (keys expected)) expected))))

(defn both-names [pred]
  (let [plain (readable/fn-string pred)
        lifted (readable/fn-string (lifting/lift pred))]
    (if (= plain lifted)
      plain
      (format "`%s` mismatches `%s`" plain lifted))))

(defn check-for-explanations [type candidate]
  (let [[retval output] (val-and-output (type/checked type candidate))]
    (if (nil? retval)
      (str/split output #"\n") ; too lazy to handle windows.
      ["Actual return result was not `nil`"])))


(defn err:required [what]
  (format "%s must exist and be non-nil" what))

(defn err:shouldbe [what should-be is]
  (format "%s should be `%s`; it is `%s`" what should-be is))

(defn is-checked [type value]
  (= (type/checked type value) value))
