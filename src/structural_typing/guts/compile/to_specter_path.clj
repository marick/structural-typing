(ns ^:no-doc structural-typing.guts.compile.to-specter-path
  (:use structural-typing.clojure.core)
  (:refer-clojure :exclude [compile])
  (:require [com.rpl.specter :as specter]
            [structural-typing.guts.self-check :as self :refer [returns-many]]
            [structural-typing.guts.type-descriptions.elements :as element]
            [structural-typing.guts.exval :as exval]
            [structural-typing.assist.oopsie :as oopsie]
            [structural-typing.guts.preds.wrap :as wrap]))

(defn path-will-match-many? [path]
  (boolean (some element/will-match-many? path)))

(defn replace-with-indices [path indices]
  (loop [result []
         [p & ps] path
         indices indices]
    (cond (nil? p)
          result

          (element/will-match-many? p)
          (recur (conj result (first indices))
                 ps
                 (rest indices))

          :else
          (recur (conj result p)
                 ps
                 indices))))


(defn- surround-with-index-collector [vec]
  (-> [(specter/view (partial map-indexed vector))]
      (into vec)
      (into [(specter/collect-one specter/FIRST)
             specter/LAST])))

(defn compile [original-path]
   (loop [[elt & remainder] original-path
          specter-path []
          path-type :constant-path]
     (cond (nil? elt)
           [(apply specter/comp-paths specter-path) path-type]

           (element/will-match-many? elt)
           (recur remainder
                  (into specter-path
                        (surround-with-index-collector (element/specter-equivalent elt)))
                  :indexed-path)

           (integer? elt)
           (recur remainder
                  (into specter-path [(specter/srange elt (inc elt)) specter/ALL])
                  path-type)

           :else
           (recur remainder (conj specter-path elt) path-type))))

(def constant-path-exval-maker exval/->ExVal)
(defn constant-path-postprocessor [specter-result oopsie] oopsie)
(defn indexed-path-exval-maker [specter-result original-path whole-value]
  (exval/->ExVal (last specter-result) original-path whole-value))
(defn indexed-path-postprocessor [specter-result raw-oopsie]
  (update raw-oopsie :path #(replace-with-indices % (butlast specter-result))))

(defn processors [path-type]
  (case path-type
    :constant-path [constant-path-exval-maker constant-path-postprocessor]
    :indexed-path [indexed-path-exval-maker indexed-path-postprocessor]
    (boom! "%s is an invalid path-type (neither constant nor indexed)" path-type)))

(defn mkfn:whole-value->oopsies [original-path lifted-preds]
  (let [[compiled-path path-type] (compile original-path)
        [exval-maker path-postprocessor] (processors path-type)]
    (fn [whole-value]
      (try
        (let [specter-results (specter/compiled-select compiled-path whole-value)]
          (for [result specter-results
                raw-oopsie (lifted-preds (exval-maker result original-path whole-value))]
            (path-postprocessor result raw-oopsie)))
        (catch Exception ex
          (vector {:explainer (constantly (format "%s is not a path into `%s`"
                                                  (oopsie/friendly-path {:path original-path})
                                                  (pr-str whole-value)))
                   ;; These are just for debugging should it be needed.
                   :whole-value whole-value
                   :path original-path}))))))
