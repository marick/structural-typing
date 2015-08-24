(ns ^:no-doc structural-typing.guts.paths.substituting
  (:require [com.rpl.specter :as specter]
            [such.function-makers :as mkfn])
  (:require [structural-typing.guts.frob :as frob]
            [structural-typing.guts.paths.elements :as element])
)


;;;;                             The (includes :Point) mechanism

(def type-finder-key ::type-finder)

(defn type-finder? [x]
  (= type-finder-key (type x)))


(defn includes
  "During creation of a type by [[named]] or [[type!]], a call to
   `includes` is replaced with the type the `type-signifier` refers to."
  [type-signifier]
  (when-not (keyword? type-signifier) (frob/boom! "%s is supposed to be a keyword." type-signifier))
  (-> (fn [type-map]
        (if-let [result (get type-map type-signifier)]
          result
          (frob/boom! "%s does not name a type" type-signifier)))
      (with-meta {:type type-finder-key})))

(defn dc:expand-type-signifiers [type-map form]
  (let [do-one #(if (type-finder? %) (% type-map) %)]
    (specter/transform (specter/walker type-finder?) do-one form)))




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
  (mkfn/lazyseq:x->abc index-collecting-splice element/will-match-many?))

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
        (frob/boom! "A map cannot be the first element of a path: `%s`" x)
        
        (not (map? (last x)))
        (frob/boom! "Nothing may follow a map within a path: `%s`" x)
        
        :else
        true))


(def dc:split-paths-ending-in-maps
  (mkfn/lazyseq:x->abc #(let [prefix-path (pop %)]
                       (vector prefix-path (hash-map prefix-path (last %))))
                    ends-in-map?))
                    
