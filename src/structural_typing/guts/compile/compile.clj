(ns ^:no-doc structural-typing.guts.compile.compile
  (:use structural-typing.clojure.core)
  (:require [structural-typing.guts.compile.to-specter-path :as to-specter-path]
            [structural-typing.guts.compile.compile-path :as path]
            [com.rpl.specter :as specter]
            [structural-typing.guts.self-check :as self :refer [returns-many]]
            [structural-typing.guts.preds.pseudopreds :as pseudo]
            [structural-typing.guts.exval :as exval]
            [structural-typing.guts.preds.wrap :as wrap]))

;;;;;;; OLD


(defn compile-predicates [preds]
  (let [lifted (map wrap/lift preds)]
    (fn [value-holder]
      (->> (reduce #(into %1 (%2 value-holder)) [] lifted)
           (returns-many :oopsie)))))

(defn compile-type [type-map]
  ;; Note that the path-checks are compiled once, returning a function to be run often.
  (let [one-check (fn [[path preds]]
                    (to-specter-path/mkfn:whole-value->oopsies path
                                                               (compile-predicates preds)))
        compiled-path-checks (map one-check type-map)]
    (fn [whole-value]
      (reduce (fn [all-errors path-check]
                (into all-errors (path-check whole-value)))
              []
              compiled-path-checks))))


;;;;;; NEW

;; (defn mkfn:whole-value->oopsies [[compiled-path compiled-preds]]
;;   (fn [whole-value]
;;     (let [starting-exval (exval/->ExVal whole-value [] whole-value)
;;           _ (prn :start-with starting-exval)
;;           exvals (path/apply-path compiled-path whole-value)]
;;       (prn :descended-to exvals)
;;       (mapcat compiled-preds exvals))))

(defn compile-pair [[path preds]]
  (vector (path/compile-path path (pseudo/max-rejection preds))
          (compile-predicates (pseudo/without-pseudopreds preds))))

(defn run-compiled-pairs [whole-value pairs]
  (reduce (fn [so-far [compiled-path compiled-preds]]
            (let [exvals (path/apply-path compiled-path whole-value)]
              (into so-far (mapcat compiled-preds exvals))))
          []
          pairs))

(defn compile-type-2 [type-map]
  (let [compiled-pairs (map compile-pair type-map)]
    (fn [whole-value]
      (run-compiled-pairs whole-value compiled-pairs))))
  ;;   (let [starting-exval
  ;;         _ (prn :start-with starting-exval)
  ;;         exvals (path/apply-path compiled-path whole-value)]
  ;;     (prn :descended-to exvals)
  ;;     (mapcat compiled-preds exvals))))
  ;; )


  ;; (let [one-check (fn [[path preds]]
  ;;                   (to-specter-path/mkfn:whole-value->oopsies path
  ;;                                                              (compile-predicates preds)))
  ;;       compiled-path-checks (map one-check type-map)]
  ;;   (fn [whole-value]
  ;;     (reduce (fn [all-errors path-check]
  ;;               (into all-errors (path-check whole-value)))
  ;;             []
  ;;             compiled-path-checks))))
