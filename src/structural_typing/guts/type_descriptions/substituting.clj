(ns ^:no-doc structural-typing.guts.type-descriptions.substituting
  (:use structural-typing.clojure.core)
  (:require [com.rpl.specter :as specter]
            [such.metadata :as meta]
            [structural-typing.guts.type-descriptions.elements :as element]))


;;;;                             The (includes :Point) mechanism

(def type-expander-key ::type-expander)

(defn type-expander? [x]
  (boolean (meta/get x type-expander-key)))

(defn as-type-expander [x]
  (meta/assoc x type-expander-key true))

(defn includes
  "During creation of a type by [[named]] or [[type!]], a call to
   `includes` is replaced with the type the `type-signifier` refers to."
  [type-signifier]
  (when-not (keyword? type-signifier) (boom! "%s is supposed to be a keyword." type-signifier))
  (-> (fn [type-map]
        (if-let [result (get type-map type-signifier)]
          result
          (boom! "%s does not name a type" type-signifier)))
      as-type-expander))

(defn dc:expand-type-signifiers [type-map form]
  (specter/transform (specter/walker type-expander?)
                     #(% type-map)
                     form))



;;;;          Replacing match-many path elements with relevant indices

(defn path-will-match-many? [path]
  (boolean (some element/will-match-many? path)))

(defn index-collecting-splice [elt]
  (let [note-index (specter/view (partial map-indexed vector)) ; value [x y] -> [ [0 x] [1 y] ]
                                                               ; for next step
        specific (element/specter-equivalent elt)  ; most often, this will splice in `ALL`
        prepend-index (specter/collect-one specter/FIRST)  ; stash the index (0 or 1 above) so that
                                                           ; Specter will prepend to final result.
        intermediate-value specter/LAST]  ; Further selectors apply to the original val (x and y)
    ;; Typical example:
    ;;    Path: [:x ALL even?]
    ;;    Input: [ {:x 100} {:x 101} {:x 102} ]
    ;;    Result [ [0 100]           [2 102] ]

    (-> [note-index]
        (into specific)
        (conj prepend-index)
        (conj intermediate-value))))

(def force-collection-of-indices
  (lazyseq:x->abc index-collecting-splice element/will-match-many?))

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


;;;                     The unadvertised required-path-ending-in-map mechanism


(defn ends-in-map? [x]
  (cond (map? x)
        false
        
        (not (some map? x))
        false
        
        (map? (first x))
        (boom! "A map cannot be the first element of a path: `%s`" x)
        
        (not (map? (last x)))
        (boom! "Nothing may follow a map within a path: `%s`" x)
        
        :else
        true))


(def dc:split-paths-ending-in-maps
  (lazyseq:x->abc #(let [prefix-path (pop %)]
                     (vector prefix-path (hash-map prefix-path (last %))))
                  ends-in-map?))
                    
