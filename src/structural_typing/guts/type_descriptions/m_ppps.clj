(ns ^:no-doc structural-typing.guts.type-descriptions.m-ppps
  "PPP is short for 'path-predicate pair': a vector + a set. Condensed
  ppps have paths that can be used to generate other ppps."
  (:use structural-typing.clojure.core)
  (:require [such.readable :as readable]
            [structural-typing.guts.type-descriptions.paths :as path]
            [structural-typing.guts.type-descriptions.multiplying :as multiply]
            [structural-typing.guts.preds.core :refer [required-key]]))

(defrecord PPP [path preds])

(def ->ppp ->PPP)

(def ^:private path-part :path)
(def ^:private preds-part :preds)

(defn mkfn:x-is? [ppp-part]
  (fn [pred] (comp pred ppp-part)))

(def path-is? (mkfn:x-is? path-part))
(def preds-is? (mkfn:x-is? preds-part))


(defn mkfn:apply-to [ppp-part]
  (fn [f ppp] (f (ppp-part ppp))))
(def apply-to-path (mkfn:apply-to path-part))
(def apply-to-preds (mkfn:apply-to preds-part))

(defn spread-path-and-x [spreader pred-creator]
  (fn [ppp]
    (map #(->ppp % (pred-creator ppp))
         (apply-to-path spreader ppp))))
  
(defn spread-path-into-required-ppps [spreader]
  (spread-path-and-x spreader (constantly #{required-key})))

(defn spread-path [spreader]
  (spread-path-and-x spreader preds-part))

(def validated-preds 
  (partial map #(if (extended-fn? %)
                  %
                  (boom! "`%s` is not a predicate." %))))

;;; description decompressors

(def dc:flatmaps->ppps 
  (lazyseq:x->abc (partial map (fn [[path preds]] (->ppp path (set (validated-preds preds)))))))

(def dc:fix-forked-paths 
  (lazyseq:x->abc (spread-path multiply/forked-paths)
                  (path-is? multiply/forking?)))

(def dc:fix-required-paths-with-collection-selectors
  (lazyseq:x->xabc (spread-path-into-required-ppps multiply/required-prefix-paths)
                   (preds-is? multiply/required?)))


;;; And the final result

(defn ->type-description [stream]
  (letfn [(make-map [stream]
            (reduce (fn [so-far ppp]
                      (update-in so-far [(:path ppp)] set-union (:preds ppp)))
                    {}
                    stream))
          (vectorize-preds [kvs]
            (update-each-value kvs
                               #(if (contains? % required-key)
                                  (into [required-key]
                                        (set-difference % #{required-key}))
                                  (vec %))))]
    (-> stream make-map vectorize-preds)))



;;; NEW

(defrecord Requires [args])
(defn requires [& args] (->Requires args))

(defmethod clojure.core/print-method Requires [o, ^java.io.Writer w]
  (.write w (readable/value-string (cons 'required (:args o)))))
          

  

(defprotocol DescriptionExpander
  (condensed-description->ppps [this]))

(extend-type Requires
  DescriptionExpander
  (condensed-description->ppps [this]
    (->> (:args this)
         (map force-vector)
         (mapcat path/->paths)
         (map #(->PPP % [required-key])))))

(extend-type clojure.lang.IPersistentMap
  DescriptionExpander
  (condensed-description->ppps [this]
    (map (fn [[k v]] (->PPP k v)) this)))
