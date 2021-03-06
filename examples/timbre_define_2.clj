(ns timbre-define-2
  "Logging to Timbre - multi-line messages that contains the whole value"
  ;; Because this is all about tailoring structural-typing, the rare `:refer :all` is appropriate:
  (:use structural-typing.type)

  (:require [structural-typing.preds :as pred]
            [structural-typing.assist.oopsie :as oopsie]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]))

(timbre/set-level! :info)

;; Example 2: print a header line, followed by N explanations

;; The explainer is given N oopsies, but they all come from
;; the same original value, called `:whole-value`.
;; Note N is always at least 1.
(defn whole-value [oopsies]
  (:whole-value (first oopsies)))

(def pprint-to-string #(with-out-str (pprint %)))

(defn error-explainer [oopsies]
  (let [intro-line "Type failure while checking this:"
        whole-value-line (pprint-to-string (whole-value oopsies))]
    (->> (oopsie/explanations oopsies)
         (into [intro-line whole-value-line])
         (str/join "\n")
         timbre/error)))

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
