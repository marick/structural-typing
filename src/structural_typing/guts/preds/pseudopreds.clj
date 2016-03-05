(ns ^:no-doc structural-typing.guts.preds.pseudopreds
  "Preds that are used througout"
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.preds.wrap :as wrap]
            [structural-typing.guts.expred :as expred]
            [structural-typing.assist.oopsie :as oopsie]
            [such.metadata :as meta]
            [defprecated.core :as depr]))

(defn rejects-missing-and-nil [pred]
  (meta/assoc pred ::special-case-handling {:reject-missing? true, :reject-nil? true}))
(defn rejects-missing [pred]
  (meta/assoc pred ::special-case-handling {:reject-missing? true, :reject-nil? false}))
(defn rejects-nil [pred]
  (meta/assoc pred ::special-case-handling {:reject-missing? false, :reject-nil? true}))

(defn special-case-handling [pred]
  (meta/get pred ::special-case-handling {}))

(defn rejects-missing-and-nil? [x]
  (= (special-case-handling x) {:reject-missing? true, :reject-nil? true}))

(def required-path
  "False iff a key/path does not exist or has value `nil`. 
   
   Note: At some point in the future, this library might make a distinction
   between a `nil` value and a missing key. If so, this predicate will change
   to accept `nil` values. See [[not-nil]].
"
  (-> (expred/->ExPred (comp not nil?)
                       "required-path"
                       #(format "%s must exist and be non-nil"
                                (oopsie/friendly-path %)))
      (wrap/lift-expred [:check-nil])
      rejects-missing-and-nil))



(def reject-missing
  "TBD"
  (rejects-missing (fn[])))

(def reject-nil
  "TBD"
  (rejects-nil (fn[])))


(defn max-rejection [preds]
  (let [raw-data (map special-case-handling preds)]
    {:reject-nil? (any? true? (map :reject-nil? raw-data))
     :reject-missing? (any? true? (map :reject-missing? raw-data))}))

(defn without-pseudopreds [preds]
  (remove #(let [rejectionism (special-case-handling %)]
             (or (:reject-nil? rejectionism)
                 (:reject-missing? rejectionism)))
          preds))


(def not-nil-fn
  "False iff a key/path does not exist or has value `nil`.

   Note: At some point in the future, this library might make a distinction
   between a `nil` value and a missing key. If so, this predicate will change
   to reject `nil` values but be silent about missing keys. See [[required-path]].
"
  (-> (expred/->ExPred (comp not nil?)
                       "not-nil"
                       #(format "%s is nil, and that makes Sir Tony Hoare sad"
                                (oopsie/friendly-path %)))
      (wrap/lift-expred [:check-nil])
      rejects-nil))

(defn not-nil [& args]
  {:deprecated {:in "2.0.0"
                :use-instead reject-nil}}
  (apply not-nil-fn args))
