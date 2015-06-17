(ns structural-typing.pipeline-stages
  (:refer-clojure :exclude [instance?])
  (:require [clojure.string :as str]
            [structural-typing.bouncer-validators :as b-in]
            [structural-typing.bouncer-errors :as b-err]))



;;; Utilities

(defn var-message [v]
  (format "%%s should be `%s`; it is `%%s`" (:name (meta v))))

;;; Various handlers

;; Step 1 is the creation of error messages

(defn default-error-explanation-producer [kvs]
  (let [{:keys [path value message]} (b-err/within-bouncer:simplify-raw-error-state kvs)]
    (if (fn? message)
      (message kvs)
      (format message
              (pr-str (if (= 1 (count path)) (first path) path))
              (pr-str value)))))

(defn default-map-adapter [explanation-map candidate]
  (b-err/nested-explanation-map->explanations explanation-map))



(def default-success-handler identity)

(defn default-failure-handler
  "This failure handler prints each error message on a separate line. It returns
   `nil`, allowing constructs like this:
   
        (some-> (type/checked :frobnoz x)
                (assoc :goodness true)
                ...)
"
  [error-explanations value-being-checked]
  (doseq [s error-explanations]
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




;; This is defined in this namespace so that it's available to both `type` and
;; `global-type` without a circular dependency.
(def ^:no-doc global-type-repo)

