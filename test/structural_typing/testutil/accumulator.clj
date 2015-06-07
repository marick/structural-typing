(ns structural-typing.testutil.accumulator
  (:refer-clojure :exclude [reset!])
  (:require [structural-typing.type :as type]))

(def ^:private accumulator (atom []))

(defn failure-handler [o]
  (swap! accumulator (constantly o))
  :failure-handler-called)

(def type-repo
  (-> type/empty-type-repo (assoc :failure-handler failure-handler)))

(defn reset! []
  (clojure.core/reset! accumulator []))

(defn messages []
  (deref accumulator))
