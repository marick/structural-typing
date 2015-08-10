(ns structural-typing.surface.mechanics
  "An interface to the main mechanisms by which parts of a type are assembled
   and used. For advanced use: writing complicated purposes, checking without
   the use of a type repo (as in Midje)."
  (:require [structural-typing.guts.preds.lifted :as lifted]
            [such.function-makers :as mkfn]
            [structural-typing.guts.preds.annotated :as annotated])
  (:use such.shorthand))

(defn mkfn:optional [pred]
  (fn [leaf-value]
    (if (nil? leaf-value)
      true
      (pred leaf-value))))



(defn lift-pred-map [about-pred & additions]
  (when-not (empty? (remove #{:catching :optional} additions))
    (throw additions))
  (let [pred-with-additions (do (-> (:predicate about-pred)
                                    (cond-> (any? #{:catching} additions) mkfn/pred:exception->false
                                            (any? #{:optional} additions) mkfn:optional)))]
    (-> (fn [kvs-about-call]
          (if (pred-with-additions (:leaf-value kvs-about-call))
            []
            (vector (lifted/->oopsie about-pred kvs-about-call))))
        lifted/mark-as-lifted
        (lifted/name-lifted-predicate (:predicate-string about-pred)))))

(defn lift
  "Convert a predicate into a function that produces either an empty
   sequence or a singleton vector containing an [[oopsie]] describing
   the failure of the predicate. However, nothing is done to an 
   already-lifted predicate.

   If the `:optional` addition is supplied, a `nil` leaf value is not given
   to `pred`. Instead, an empty sequence (success) is returned.

   If the `:catching` addition is supplied, any exception is caught and
   an oopsie is returned.

   The lifted predicate takes a map that must contain at least
   `:leaf-value`.
"
  [pred & additions]
  (if (lifted/already-lifted? pred)
    pred
    (apply lift-pred-map (lifted/pred->about-pred pred) additions)))
