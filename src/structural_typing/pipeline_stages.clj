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

(def empty-type-repo
  "A repository that contains no type descriptions. It contains
   default behavior for both success and failure cases. Here's
   an example of changing the behavior and adding a type:
   
         (-> type/empty-type-repo
             (assoc :failure-handler type/throwing-failure-handler)
             (type/named :frobable [:left :right :arrow]))
"
  {:failure-handler default-failure-handler
   :success-handler default-success-handler
   :bouncer-map-adapter default-bouncer-map-adapter
   :error-string-producer default-error-string-producer
   })


(def global-type-repo)

