(ns structural-typing.assist.testutil
  "Useful shorthand for tests of new code, like custom predicates."
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.exval :as exval]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.assist.lifting :as lifting]
            [structural-typing.type :as type]
            [such.readable :as readable]
            such.immigration
            structural-typing.guts.explanations))

(defn exval
  "Generate an \"extended value\". Omitted values are replaced
   with useful defaults.
   
         (exval 5 :x) => (exval 5 :x {:x 5})
         (exval 5) => (exval 5 :x {:x 5})
"
  ([leaf-value path whole-value]
     (exval/->ExVal leaf-value path whole-value))
  ([leaf-value path]
     (exval leaf-value path (hash-map path leaf-value)))
  ([leaf-value]
     (exval leaf-value [:x])))

(defn lift-and-run
  "[[lift-pred]] the `pred` and run it against the [[exval]]."
  [pred exval]
  ( (lifting/lift-pred pred) exval))

(defn explain-lifted
  "[[lift-and-run]] the predicate against the [[expred]], then
   generate a list of [[explanations]].
   Note that it's safe to use this on an already-lifted predicate."
  [pred exval]
  (oopsie/explanations ((lifting/lift-pred pred) exval)))


;; Don't use Midje checkers to avoid dragging in all of its dependencies

(defn oopsie-for 
  "Create a function that takes an [[oopsie]] and checks it.
   Examples:
   
        ... => (oopsie-for 5) ; :leaf-value must be 5, other keys irrelevant
        ... => (oopsie-for 5 :whole-value {:x 5})  ; two keys relevant, others not.
"
  [leaf-value & {:as kvs}]
  (let [expected (assoc kvs :leaf-value leaf-value)]
    (fn [actual]
      (= (select-keys actual (keys expected)) expected))))

(defn both-names 
  "Generate the readable names of both the original and lifted predicates.
   Provoke a test failure if they're not the same. Otherwise, return the value
   for further checking.
   
        (both-names (member [1 2 3])) => \"(member [1 2 3])\"
"
  [pred]
  (let [plain (readable/fn-string pred)
        lifted (readable/fn-string (lifting/lift-pred pred))]
    (if (= plain lifted)
      plain
      (format "`%s` mismatches `%s`" plain lifted))))

(defn check-for-explanations
  "Run [[built-like]] against the arguments. The result is supposed to be
   error messages and a `nil` return value. If not, provoke an error. If so,
   return the explanations.
   
        (check-for-explanations :Figure {:points 3}) => (just (err:missing :color))
"
  [& args]
  (let [[retval output] (val-and-output (apply type/built-like args))]
    (if (nil? retval)
      (str-split output #"\n") ; too lazy to handle windows.
      ["Actual return result was not `nil`"])))

(defn check-all-for-explanations
  "Same as [[check-for-explanations]] but uses `all-built-like`."
  [& args]
  (let [[retval output] (val-and-output (apply type/all-built-like args))]
    (if (nil? retval)
      (str-split output #"\n") ; too lazy to handle windows.
      ["Actual return result was not `nil`"])))


(import-vars [structural-typing.guts.explanations
                err:not-maplike
                err:not-collection
                err:not-sequential
                err:maplike
                err:selector-at-nil
                err:value-nil
                err:missing])

(defn err:shouldbe
  "Produces the same error messsage produced when a predicate not altered by [[explain-with]]
   fails."
  ([path should-be is]
     (err:shouldbe path should-be is false))

  ([path should-be is omit-quotes]
     (let [should-be (if omit-quotes should-be (str "`" should-be "`"))]
       (format "%s should be %s; it is `%s`" path should-be is))))
