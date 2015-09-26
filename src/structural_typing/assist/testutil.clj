(ns structural-typing.assist.testutil
  "Useful shorthand for tests for new code, like custom predicates."
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.exval :as exval]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.assist.lifting :as lifting]
            [structural-typing.type :as type]
            [such.readable :as readable]))

(defn exval
  ([leaf-value path whole-value]
     (exval/->ExVal leaf-value path whole-value))
  ([leaf-value path]
     (exval leaf-value path (hash-map path leaf-value)))
  ([leaf-value]
     (exval leaf-value [:x])))

(defn lift-and-run [pred exval]
  ( (lifting/lift-pred pred) exval))

(defn explain-lifted
  "Note that it's safe to use this on an already-lifted predicate"
  [pred exval]
  (oopsie/explanations ((lifting/lift-pred pred) exval)))


;; Don't use Midje checkers to avoid dragging in all of its dependencies

(defn oopsie-for [leaf-value & {:as kvs}]
  (let [expected (assoc kvs :leaf-value leaf-value)]
    (fn [actual]
      (= (select-keys actual (keys expected)) expected))))

(defn both-names [pred]
  (let [plain (readable/fn-string pred)
        lifted (readable/fn-string (lifting/lift-pred pred))]
    (if (= plain lifted)
      plain
      (format "`%s` mismatches `%s`" plain lifted))))

(defn check-for-explanations [& args]
  (let [[retval output] (val-and-output (apply type/built-like args))]
    (if (nil? retval)
      (str-split output #"\n") ; too lazy to handle windows.
      ["Actual return result was not `nil`"])))


(defn err:required [what]
  (format "%s must exist and be non-nil" what))

(defn err:shouldbe [what should-be is]
  (format "%s should be `%s`; it is `%s`" what should-be is))

(defn is-built-like [type value]
  (= (type/built-like type value) value))
