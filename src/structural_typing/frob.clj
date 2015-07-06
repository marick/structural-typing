(ns ^:no-doc structural-typing.frob
  "General-purpose functions for frobbing data in various ways.")

(defn extended-fn?
  "`fn?` does not consider multimethods to be functions. This does."
  [x]
  (or (fn? x)
      (instance? clojure.lang.MultiFn x)))
  
(defn boom [fmt & args]
  (throw (new RuntimeException (apply format fmt args))))

(defn update-each-value [kvs f & args]
  (reduce (fn [so-far k] 
            (assoc so-far k (apply f (get kvs k) args)))
          kvs
          (keys kvs)))

(defn mkmap:all-keys-with-value [keys v]
  (reduce (fn [so-far k]
            (assoc so-far k v))
          {}
          keys))


(defn wrap-pred-with-catcher [f]
  (fn [& xs]
    (try (apply f xs)
    (catch Exception ex false))))

(defn force-vector [v]
  (cond (vector? v) v
        (sequential? v) (vec v)
        :else (vector v)))

(defn adding-on [coll maybe-vector]
  (into coll (force-vector maybe-vector)))

(defn mkfn:mkst [prefixer]
  (fn two-arg-form
    ([transformer pred]
       (fn lazily-handle [[x & xs :as lazyseq]]
         (lazy-seq 
          (cond (empty? lazyseq)
                nil
                          
                (pred x)
                ((prefixer x (transformer x)) (lazily-handle xs))
                          
                :else
                (cons x (lazily-handle xs))))))
    ([transformer]
       (two-arg-form transformer (constantly true)))))

(def mkst:x->abc
  "Takes a transformer function and optional predicate, which defaults to `(constantly true)`.
   The transformer function must produce a collection. 
   Produces a function that converts one lazy sequence into another.
   For each element of the input sequence:
   * If `pred` is falsey, the unchanged element is in the output sequence.
   * If `pred` is truthy, the transformed element is \"spliced\" into the output
     sequence in replace of the original.
   
           (let [replace-with-N-copies (mkst:x->abc #(repeat % %))]
             (replace-with-n-copies [0 1 2 3]) => [1 2 2 3 3 3])

           (let [replace-evens-with-N-copies (mkst:x->abc #(repeat % %) even?)]
             (replace-with-n-copies [0 1 2 3]) => [1 2 2 3])
"
  (mkfn:mkst (fn [x tx] #(concat tx %))))

(def mkst:x->xabc (mkfn:mkst (fn [x tx] #(cons x (concat tx %)))))
(def mkst:x->y (mkfn:mkst (fn [x tx] #(cons tx %))))
            


            
(defn mkst:validator [pred exploder]
  (fn [coll]
    (map #(if (pred %) % (exploder coll %)) coll)))
