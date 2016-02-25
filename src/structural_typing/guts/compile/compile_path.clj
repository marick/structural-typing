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
            [structural-typing.assist.oopsie :as oopsie]
            [slingshot.slingshot :refer [throw+ try+]]))

;; NAMESPACE NOTES

;; Specter requires `extend-type/extend-protocol` instead of defining
;; the protocol functions in the deftype. It's an implementation
;; detail.


;;;; Support

(defn no-transform! []
  (boom! "structural-typing does not use transform"))

(defn- associative-select* [this {:keys [leaf-value path] :as structure} next-fn]
  (let [original-key (:key-given-in-path this)]
    (cond (not (map? leaf-value))
          (-> structure
              (assoc :path (conj path original-key))
              explain/oopsies:shouldbe-maplike)

          (and (not (contains? leaf-value original-key))
               (:reject-missing? this))
          (-> structure
              (assoc :path (conj path original-key))
              explain/oopsies:missing)

          (and (not (get leaf-value original-key))
               (:reject-nil? this))
          (-> structure
              (assoc :path (conj path original-key))
              explain/oopsies:nil)

          :else
          (-> structure
              (assoc :leaf-value (get leaf-value original-key)
                     :path (conj path original-key))
              next-fn))))

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
  (select* [this {:keys [leaf-value path] :as structure} next-fn]
    (cond (and (nil? leaf-value)
               (or (:reject-missing? this)
                   (:reject-nil? this)))
          (-> structure
              (assoc :path (conj path this))
              explain/oopsies:all-missing)

          (nil? leaf-value)
          []

          (not (coll? leaf-value))
          (-> structure
              (assoc :path (conj path this))
              explain/oopsies:shouldbe-collection)

          (map? leaf-value)
          (-> structure
              (assoc :path (conj path this))
              explain/oopsies:shouldbe-not-maplike)

          :else
          (->> leaf-value
               (map-indexed vector)
               (mapcat (fn [[index new-leaf]]
                         (-> structure
                             (assoc :path (conj path index)
                                    :leaf-value new-leaf)
                             next-fn))))))
  (transform* [& _] (no-transform!)))

(def ALL (->AllPathElement))

(defmethod clojure.core/print-method AllPathElement [o, ^java.io.Writer w] (.write w "ALL"))
(readable/instead-of ALL 'ALL)



;;;; Workers


;; This could possibly be handled with protocol cleverness, but:
;; 1. that would require even more knowledge of the classes behind clojure types, and
;; 2. there's some value in having everything in one place.
(defn path-element [signifier]
  (cond (keyword? signifier)
        (->KeywordPathElement signifier)

        (string? signifier)
        (->StringPathElement signifier)

        (= ALL)
        (->AllPathElement)

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
