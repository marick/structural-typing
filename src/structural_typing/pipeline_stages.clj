(ns structural-typing.pipeline-stages
  (:refer-clojure :exclude [instance?])
  (:require [clojure.string :as str]
            [bouncer.core :as b]
            [structural-typing.validators :as v]))

(def default-success-handler identity)

(defn default-failure-handler
  "This failure handler prints each error message on a separate line. It returns
   `nil`, allowing constructs like this:
   
        (some-> (type/checked :frobnoz x)
                (assoc :goodness true)
                ...)
"
  [messages]
  (doseq [s messages]
    (println s))
  nil)

(defn throwing-failure-handler 
  "In contrast to the default failure handler, this one throws a
   `java.lang.Exception` whose message is the concatenation of the
   type-mismatch messages.
   
   To make all type mismatches throw failures, do this:
   
          (type/set-failure-handler! type/throwing-failure-handler)
"
  [messages]
  (throw (new Exception (str/join "\n" messages))))



(defn default-bouncer-map-adapter [error-map checked-map]
  (flatten (vals error-map)))

(defn default-error-string-producer [{path :path, value :value optional-message-arg :message
                                      {default-message-format :default-message-format} :metadata
                                      :as kvs}]
  (let [handler (or optional-message-arg default-message-format
                    "configuration error: no message format")]
    (if (fn? handler)
      (handler kvs)
      (format handler
              (pr-str (if (= 1 (count path)) (first path) path))
              (pr-str value)))))



;; This is defined in this namespace so that it's available to both `type` and
;; `global-type` without a circular dependency.
(def ^:no-doc global-type-repo)

