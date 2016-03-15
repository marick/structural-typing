(ns timbre-define-3
  "Logging to Timbre - as with 2, but each line in a different log message"
  ;; Because this is all about tailoring structural-typing, the rare `:refer :all` is appropriate:
  (:use structural-typing.type)

  (:require [structural-typing.preds :as pred]
            [structural-typing.assist.oopsie :as oopsie]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]))

(timbre/set-level! :info)

;; Example 3: print multiple individual messages, followed by a summary

(def pprint-to-string #(with-out-str (pprint %)))

(defn error-explainer [oopsies]
  (timbre/info "While checking this:")
  (-> (first oopsies) ; the error handler is always given at least one oopsie.
      :whole-value    ; the original candidate being checked
      pprint-to-string
      str/trimr       ; be tidy by getting rid of pprint's trailing newline
      timbre/info)
  (doseq [e (oopsie/explanations oopsies)] (timbre/info e))
  (timbre/error "Type check failed - see preceding messages."))

(def type-repo
  (-> empty-type-repo
      (named :Point
             (requires :x :y)
             {:x integer? :y integer?})
      (replace-error-handler error-explainer)))

;; Define namespace-local versions of the standard functions that default to this type-repo.
;; The standard functions are `built-like`, `all-built-like`, `<>built-like`,
;; `<>all-built-like`, and `built-like?`.

(ensure-standard-functions type-repo)

;; For example, clients can use this:
;;     (mytypes/built-like :Point x)
