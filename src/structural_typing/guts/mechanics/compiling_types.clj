(ns ^:no-doc structural-typing.guts.mechanics.compiling-types
  (:refer-clojure :exclude [compile])
  (:require [blancas.morph.monads :as e]
            [com.rpl.specter :as specter]
            [structural-typing.surface.oopsie :as oopsie]
            [structural-typing.guts.paths.substituting :as path]
            [structural-typing.guts.preds.lifted :refer [lift]]))

(defn compile-predicates [preds]
  (let [lifted (map lift preds)
        combined (apply juxt lifted)]
    (comp e/lefts combined)))

;; TODO: This code could be made tenser. It cries out for transients.

(defn oopsies-for-bad-path [whole-value original-path]
  (let [base-oopsie {:whole-value whole-value
                     :original-path original-path
                     :path original-path
                     :leaf-value nil}]
    (-> base-oopsie
        (assoc :predicate-explainer (constantly 
                                     (format "%s is not a path into `%s`"
                                             ;; ick.
                                             (oopsie/friendly-path base-oopsie)
                                             (pr-str whole-value))))
        vector)))


(defprotocol CompiledPath
  ;; TODO: is there a way to override the constructor?
  (compile [this])
  (run-preds [this preds specter-result])
  (assoc-path [this kvs specter-result])
  )

(defn select [compiled-path object-to-check]
  (e/make-either (specter/compiled-select (:selecter compiled-path) object-to-check)))

(defn mkfn:oopsies-for-one-specter-result [compiled-path compiled-preds]
  (fn [whole-value specter-result]
    (->> (run-preds compiled-path compiled-preds specter-result)
         (map #(assoc % :whole-value whole-value))
         (map #(assoc-path compiled-path % specter-result)))))

(defrecord SingleValueCompiledPath [original-path]
  CompiledPath
  (compile [this]
    (assoc this :selecter (apply specter/comp-paths original-path)))
  (run-preds [this preds specter-result]
    (preds {:leaf-value specter-result}))
  (assoc-path [this kvs specter-result]
    (assoc kvs :path original-path))
  )

(defrecord MultipleValueCompiledPath [original-path]
  CompiledPath
  (compile [this]
    (assoc this
           :selecter (apply specter/comp-paths (path/force-collection-of-indices original-path))
           :indices-for-path-elements-that-match-many (path/replacement-points original-path)))
  (run-preds [this preds specter-result]
    (preds {:leaf-value (last specter-result)}))
  (assoc-path [this kvs specter-result]
    (let [indices-of-checked-element (butlast specter-result)]
      (assoc kvs
             :path (path/replace-with-indices original-path
                                              (:indices-for-path-elements-that-match-many this)
                                              indices-of-checked-element))))
)

(defn ->CompiledPath [original-path]
  (let [constructor (if (path/path-will-match-many? original-path)
                      ->MultipleValueCompiledPath
                      ->SingleValueCompiledPath)]
    (-> (constructor original-path) compile)))

;; Previous and following functions are factored wrong.

(defn compile-path-check [[original-path preds]]
  (let [compiled-preds (compile-predicates preds)
        compiled-path (->CompiledPath original-path)
        ->oopsies (mkfn:oopsies-for-one-specter-result compiled-path compiled-preds)]
    (fn [whole-value]
      (e/either [specter-results-to-check (select compiled-path whole-value)]
                (oopsies-for-bad-path whole-value original-path)
                (mapcat #(->oopsies whole-value %1) specter-results-to-check)))))

(defn compile-type [t]
  ;; Note that the path-checks are compiled once, returning a function to be run often.
  (let [compiled-path-checks (map compile-path-check t)]
    (fn [object-to-check]
      (reduce (fn [all-errors path-check]
                (into all-errors (path-check object-to-check)))
              []
              compiled-path-checks))))
