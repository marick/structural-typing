(ns structural-typing.guts.paths.substituting
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
   `includes` is replaced with the type the `type-signifier` refers
   to.  The exact meaning of that replacement depends on whether it's used in a path, as
   the value of a path, or as an entire argument itself. See the wiki
   documentation."
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

(defn replacement-points [path]
  (->> path
       (map vector (range))
       (filter (comp element/will-match-many? second))
       (map first)))

(def indexed (partial map-indexed vector))

(defn index-collecting-splice [elt]
  (vector (specter/view indexed)
          elt
          (specter/collect-one specter/FIRST)
          specter/LAST))

(def force-collection-of-indices
  (mkfn/lazyseq:x->abc index-collecting-splice element/will-match-many?))

(defn replace-with-indices [path replacement-points indices]
  (assert (= (count replacement-points) (count indices)))
  (loop [result path
         [r & rs] replacement-points
         [i & is] indices]
    (if r 
      (recur (assoc result r i) rs is)
      result)))




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
                    
