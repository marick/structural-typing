(ns ^:no-doc structural-typing.guts.mechanics.compiling-types
  (:refer-clojure :exclude [compile])
  (:require [com.rpl.specter :as specter]
            [blancas.morph.monads :as e] ; for Either monad
            [structural-typing.surface.oopsie :as oopsie]
            [structural-typing.guts.paths.substituting :as path]
            [structural-typing.surface.mechanics :as mechanics]))

(defn compile-predicates [preds]
  (let [lifted (map #(mechanics/lift %) preds)]
    (fn [value-holder]
      (reduce #(into %1 (%2 value-holder))
              []
              lifted))))

;; TODO: This code could be made tenser. It cries out for transients.

(defn oopsies-for-bad-path [exval]
  (let [expred (oopsie/->ExPred :unused :unused
                                (constantly (format "%s is not a path into `%s`"
                                                    (oopsie/friendly-path exval)
                                                    (pr-str (:whole-value exval)))))]
    (vector (oopsie/->oopsie exval expred))))

(defprotocol CompiledPath
  ;; TODO: is there a way to override the constructor?
  (compile [this])
  (run-preds [this preds specter-result])
  (assoc-path [this kvs specter-result])
  )

(defn select [compiled-path object-to-check]
  ;; This is just a convenient way to trap exceptions Specter throws
  ;; It would also produce a Left for a nil result, which Specter never
  ;; returns. If I'm wrong about that, I'd rather see an error than have
  ;; it be treated as an empty array.
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
           :selecter (apply specter/comp-paths (path/force-collection-of-indices original-path))))
  (run-preds [this preds specter-result]
    (preds {:leaf-value (last specter-result)}))
  (assoc-path [this kvs specter-result]
    (let [indices-of-checked-element (butlast specter-result)]
      (assoc kvs
             :path (path/replace-with-indices original-path indices-of-checked-element))))
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
      (let [exval (oopsie/->ExVal original-path whole-value :unfilled)]
        (e/either [specter-results-to-check (select compiled-path whole-value)]
                  (oopsies-for-bad-path exval)
                  (mapcat #(->oopsies whole-value %1) specter-results-to-check))))))

(defn compile-type [t]
  ;; Note that the path-checks are compiled once, returning a function to be run often.
  (let [compiled-path-checks (map compile-path-check t)]
    (fn [object-to-check]
      (reduce (fn [all-errors path-check]
                (into all-errors (path-check object-to-check)))
              []
              compiled-path-checks))))
