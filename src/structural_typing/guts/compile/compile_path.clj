(ns ^:no-doc structural-typing.guts.compile.compile-path
  (:use structural-typing.clojure.core)
  (:refer-clojure :exclude [compile])
  (:require [com.rpl.specter :as specter]
            [com.rpl.specter.protocols :as sp]
            [clojure.core.reducers :as r]
            [such.readable :as readable]
            [structural-typing.guts.self-check :as self :refer [returns-many built-like]]
            [structural-typing.guts.explanations :as explain]
            [structural-typing.guts.exval :as exval]
            [structural-typing.guts.expred :as expred]
            [structural-typing.guts.preds.wrap :as wrap]
            [structural-typing.assist.oopsie :as oopsie]))

;; NAMESPACE NOTES

;; Specter requires `extend-type/extend-protocol` instead of defining
;; the protocol functions in the deftype. It's an implementation
;; detail.


;;;; Support

(defn update-exval [exval updates]
  (reduce-kv (fn [exval k v]
               (case k
                 :conj-path (update exval :path conj v)
                 :descend-into (assoc exval :leaf-value v)))
             exval
             updates))

(defn errcase [err:fn exval updates]
  (-> exval
      (update-exval updates)
      err:fn))


(defn no-transform! []
  (boom! "structural-typing does not use transform"))

(defn rejected-nil? [value selector-element]
  (and (nil? value)
       (:reject-nil? selector-element)))

(defn rejected-missing? [present? selector-element]
  (and (not present?)
       (:reject-missing? selector-element)))


(defn- associative-select* [this {:keys [leaf-value] :as exval} next-fn]
  (let [original-key (:key-given-in-path this)]
    (cond (rejected-nil? leaf-value this)
          (errcase explain/oopsies:selector-at-nil exval {:conj-path original-key})

          (nil? leaf-value)
          (next-fn (update-exval exval {:conj-path original-key
                                        :descend-into nil}))

          (not (map? leaf-value))
          (errcase explain/oopsies:not-maplike exval {:conj-path original-key})

          (rejected-missing? (contains? leaf-value original-key) this)
          (errcase explain/oopsies:missing exval {:conj-path original-key})

          (rejected-nil? (get leaf-value original-key) this)
          (errcase explain/oopsies:value-nil exval {:conj-path original-key})

          :else
          (next-fn (update-exval exval {:conj-path original-key
                                        :descend-into (get leaf-value original-key)})))))

;; keywords
(defrecord KeywordPathElement [key-given-in-path])

(extend-type KeywordPathElement
  sp/StructurePath
  (select* [& args] (apply associative-select* args))
  (transform* [& _] (no-transform!)))

(defmethod clojure.core/print-method KeywordPathElement [o, ^java.io.Writer w]
  (.write w (str (:key-given-in-path o))))


;; strings
(defrecord StringPathElement [key-given-in-path])

(extend-type StringPathElement
  sp/StructurePath
  (select* [& args] (apply associative-select* args))
  (transform* [& _] (no-transform!)))

(defmethod clojure.core/print-method StringPathElement [o, ^java.io.Writer w]
  (.write w (pr-str (:key-given-in-path o))))

;; ALL

(defrecord AllPathElement [])

(extend-type AllPathElement
  sp/StructurePath
  (select* [this {:keys [leaf-value] :as exval} next-fn]
    (cond (rejected-nil? leaf-value this)
          (errcase explain/oopsies:selector-at-nil exval {:conj-path this})

          (nil? leaf-value)
          []

          (not (coll? leaf-value))
          (errcase explain/oopsies:not-collection exval {:conj-path this})

          (map? leaf-value)
          (errcase explain/oopsies:maplike exval {:conj-path this})

          :else
          (->> leaf-value
               (map #(update-exval exval {:conj-path %1 :descend-into %2}) (range))
               (mapcat #(if (rejected-nil? (:leaf-value %) this)
                          (errcase explain/oopsies:value-nil % {})
                          (next-fn %))))))
  (transform* [& _] (no-transform!)))

(def ALL (->AllPathElement))

(defmethod clojure.core/print-method AllPathElement [o, ^java.io.Writer w] (.write w "ALL"))
(readable/instead-of ALL 'ALL)


;; RANGE

(defrecord RangePathElement [inclusive-start exclusive-end bounds desired-count printable])
(defmethod clojure.core/print-method RangePathElement [o, ^java.io.Writer w]
  (.write w (:printable o)))

(letfn [(desired-range [{:keys [inclusive-start desired-count]} sequence]
          ;; This works with infinite sequences
          (->> sequence
               (drop inclusive-start)
               (take desired-count)
               vec))

        (extract-desired-range-as-exvals [exval {:keys [inclusive-start] :as selector-element}]
          (->> (:leaf-value exval)
               (desired-range selector-element)
               (mapv #(update-exval exval {:conj-path %1 :descend-into %2})
                     (drop inclusive-start (range)))))

        (indexes-of-out-of-range-elements [actual-count {:keys [inclusive-start desired-count]}]
          (let [shortfall (- desired-count actual-count)]
            (take shortfall
                  (drop (+ inclusive-start actual-count)
                        (range)))))

        (padding:nil-exval [exval index]
          (update-exval exval {:conj-path index :descend-into nil}))

        (padding:missing-oopsie [exval index]
          (first (errcase explain/oopsies:missing exval {:conj-path index
                                                         :descend-into ::missing-value})))

        (pad-the-end [actual-values exval {:keys [:reject-missing?] :as selector-element}]
          (let [missing-indexes (indexes-of-out-of-range-elements (count actual-values)
                                                                  selector-element)
                padder (if reject-missing? padding:missing-oopsie padding:nil-exval)]
            (into actual-values (map padder (repeat exval) missing-indexes))))

        (perhaps-rejecting-nil [mixture selector-element]
          (mapv #(if (rejected-nil? (:leaf-value %) selector-element)
                   (first (errcase explain/oopsies:value-nil % {}))
                   %)
                mixture))

        (descend [mixture next-fn]
          (reduce (fn [so-far x]
                    (if (wrap/oopsie? x)
                      (conj so-far x)
                      (into so-far (next-fn x))))
                  []
                  mixture))]


  (defn range-select* [{:keys [inclusive-start desired-count] :as selector-element} {:keys [leaf-value] :as exval} next-fn]
    (cond (rejected-nil? leaf-value selector-element)
          (errcase explain/oopsies:selector-at-nil exval {:conj-path selector-element})

          (nil? leaf-value)
          (pad-the-end [] exval selector-element)

          (not (sequential? leaf-value))
          (errcase explain/oopsies:not-sequential exval {:conj-path selector-element})

          :else
          (-> exval
              (extract-desired-range-as-exvals selector-element)
              (pad-the-end exval selector-element)
              (perhaps-rejecting-nil selector-element)
              (descend next-fn)))))

(extend-type RangePathElement
  sp/StructurePath
  (select* [this exval next-fn]
    (range-select* this exval next-fn))
  (transform* [& _] (no-transform!)))


(letfn [(range-boom! [fmt [inclusive-start exclusive-end]]
          (boom! fmt (format "`(RANGE %s %s)`" inclusive-start exclusive-end)))]

  (defn RANGE
    "Use this in a path to select a range of values in a
     collection. The first argument is inclusive; the second exclusive.

         (type! :ELEMENTS-1-AND-2-ARE-EVEN {[(RANGE 1 3)] even?})
    "
    [inclusive-start exclusive-end]
    (let [bounds [inclusive-start exclusive-end]]
      (if (not (every? integer? bounds))
        (range-boom! "Every argument to %s should be an integer" bounds)
        (let [desired-count (- exclusive-end inclusive-start)]
          (cond (>= inclusive-start exclusive-end)
                (range-boom! "Second argument of %s should be greater than the first" bounds)

                (neg? inclusive-start)
                (range-boom! "%s has a negative lower bound" bounds)

                :else
                (->RangePathElement inclusive-start exclusive-end bounds desired-count
                                    (format "(RANGE %s %s)" inclusive-start exclusive-end))))))))

;; integers

(defrecord IntegerPathElement [index])

(extend-type IntegerPathElement
  sp/StructurePath
  (select* [this exval next-fn]
    (let [inclusive-start (:index this)
          exclusive-end (inc inclusive-start)]
      (range-select* (map->RangePathElement {:inclusive-start inclusive-start
                                             :exclusive-end exclusive-end
                                             :bounds [inclusive-start exclusive-end]
                                             :desired-count 1
                                             :printable (str inclusive-start)
                                             :reject-missing? (:reject-missing? this)
                                             :reject-nil? (:reject-nil? this)})
                     exval
                     next-fn)))
  (transform* [& _] (no-transform!)))

(defmethod clojure.core/print-method IntegerPathElement [o, ^java.io.Writer w]
  (.write w (str (:index o))))





;;;; Main API


;; This could possibly be handled with protocol cleverness, but:
;; 1. that would require even more knowledge of the classes behind clojure types, and
;; 2. there's some value in having everything in one place.
(defn path-element [signifier]
  (cond (keyword? signifier)
        (->KeywordPathElement signifier)

        (string? signifier)
        (->StringPathElement signifier)

        (integer? signifier)
        (->IntegerPathElement signifier)

        (instance? AllPathElement signifier)
        signifier

        (instance? RangePathElement signifier)
        signifier

        :else
        (boom! "`%s` is not something that can appear in a Structural-Typing path" signifier)))

(defn compile-path [path nil-and-missing-handling]
  (->> path
       (map path-element)
       (map #(merge % nil-and-missing-handling))
       specter/comp-paths))

(defn apply-path [compiled-path whole-value]
  (specter/compiled-select compiled-path
                           (exval/map->ExVal {:leaf-value whole-value
                                              :whole-value whole-value
                                              :path []})))
