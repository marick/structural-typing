(ns structural-typing.type-repo
  (:require [structural-typing.frob :as frob]
            [structural-typing.predicates :as p]
            [com.rpl.specter :as specter]))


(defn nested-map->path-map
  ([kvs parent-path]
     (letfn [(splice [maybe-vector]
               (into parent-path (frob/force-vector maybe-vector)))]
       (reduce (fn [so-far [k v]]
                 (if (map? v)
                     (merge so-far (nested-map->path-map v (splice k)))
                     (assoc so-far (splice k) (frob/force-vector v))))
               {}
               kvs)))
  ([kvs]
     (nested-map->path-map kvs [])))

(defn expand-path-shorthand
  "Expand a vector containing path elements + shorthand for alternatives into 
   a vector of paths"
  ([value]
     (expand-path-shorthand value [[]]))
       
  ([[x & xs :as value] parent-paths]
     (let [x (first value)
           xs (rest value)]
       (cond (empty? value)
             parent-paths

             (sequential? x)
             (let [extended (for [pp parent-paths, elt x]
                              (conj pp elt))]
               (expand-path-shorthand xs (vec extended)))
             
             (and (map? x) (empty? xs))
             (for [pp parent-paths, elt (keys (nested-map->path-map x []))]
               (into pp (frob/force-vector elt)))

             (map? x)
             (frob/boom "The map must be the last element of the vector: %s." value)
             
             :else
             (let [extended (for [pp parent-paths] (conj pp x))]
               (expand-path-shorthand xs (frob/force-vector extended)))))))

;;;;

(defn type-finder? [x]
  (= ::type-finder (type x)))

(defn force-literal [v type-map]
  (if (type-finder? v) (v type-map) v))

(defn a [type-key]
  (when-not (keyword? type-key) (frob/boom "%s is supposed to be a keyword." type-key))
  (-> (fn type-finder [type-map]
        (if-let [result (get type-map type-key)]
          result
          (frob/boom "%s does not name a type" type-key)))
      (with-meta {:type ::type-finder})))
(def an a)

(defn expand-type-finders [type-map form]
  (specter/update (specter/walker type-finder?)
                  #(force-literal % type-map)
                  form))



(defn each-path-must-exist [paths]
  (reduce (fn [so-far path]
            (assoc so-far path p/must-exist))
          {}
          paths))

(defn canonicalize-core [signifiers]
  (letfn [(expand-top-level-shorthand [signifier]
            (if (coll? signifier) signifier (vec signifier)))

          (handle-one [signifier]
            (cond (map? signifier)
                  (nested-map->path-map signifier)

                  (vector? signifier)
                  (->> signifier
                       (mapcat expand-path-shorthand)
                       each-path-must-exist)

                  :else
                  (throw (ex-info "A type signifier must be a map or vector" {:actual signifier}))))]

    ;; (->> signifiers
    ;;      (map frob/force-vector)
    ;;      (map handle-one)
    ;;      (apply merge-with into))))


           (->> signifiers
;                (map expand-top-level-shorthand)
                (map handle-one)
                (apply merge-with into))))
;    (apply merge-with into (map handle-one signifiers))))
  


(defn expand-type-references [type-repo descriptions]
  descriptions)

(defn undo-singleton-path-convenience [x]
  (if (sequential? x) x (vector x)))

(defn any-required-seq->maps [description]
  (cond (map? description)
        description
        
        (sequential? description)
        (frob/mkmap:all-keys-with-value (mapcat expand-path-shorthand (map undo-singleton-path-convenience description))
                                        (vector p/must-exist))
        
        :else
        1))
  ;; (if (sequential? description)
    ;; (specter/update [sequential?]
    ;;                 #(-> %
    ;;                      expand-path-shorthand
    ;;                      (map paths->map)))

(defn deconvenience-description [signifier]
  (cond (vector signifier)
        (mapv #(if (or (vector? %) (map? %))
                 %
                 (vector %))
              signifier)
        :else
        signifier))

(defn validate [description]
  (when-not (or (map? description)
                (sequential? description))
    (frob/boom "Types are described with maps or vectors: %s" description))
  description)


  
(defn canonicalize [type-repo & condensed-type-descriptions]
  (->> condensed-type-descriptions
       (expand-type-references type-repo)
       (map validate)
       (map any-required-seq->maps)
       (map nested-map->path-map)
       (apply merge-with into)))
