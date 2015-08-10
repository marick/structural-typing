(ns structural-typing.surface.mechanics
  "An interface to the main mechanisms by which parts of a type are assembled
   and used. For advanced use: writing complicated purposes, checking without
   the use of a type repo (as in Midje). A WORK IN PROGRESS.
"
  (:require [structural-typing.guts.preds.lifted :as lifted]
            [such.function-makers :as mkfn]
            [structural-typing.guts.preds.annotated :as annotated])
  (:use such.shorthand))

(defn mkfn:optional [pred]
  (fn [leaf-value]
    (if (nil? leaf-value)
      true
      (pred leaf-value))))


(defn protect-pred [pred subtractions]
  (-> pred
      (cond-> (not (any? #{:allow-exceptions} subtractions)) mkfn/pred:exception->false
              (not (any? #{:check-nil} subtractions)) mkfn:optional)))

(defn lift-pred-map
  [about-pred & subtractions]
  (when-not (empty? (remove #{:allow-exceptions :check-nil} subtractions))
    (throw subtractions))
  (let [protected (protect-pred (:predicate about-pred) subtractions)]
    (-> (fn [kvs-about-call]
          (if (protected (:leaf-value kvs-about-call))
            []
            (vector (lifted/->oopsie about-pred kvs-about-call))))
        lifted/mark-as-lifted
        (lifted/name-lifted-predicate (:predicate-string about-pred)))))

(defn lift
  "Convert a predicate into a function that produces either an empty
   sequence or a singleton vector containing an [[oopsie]] describing
   the failure of the predicate. However, nothing is done to an 
   already-lifted predicate.

   Normally, the predicate is wrapped so that (1) a `nil` value is considered
   `true`, and (2) an exception during evaluation is considered `false`. These
   can be omitted with the `:check-nil` and `:allow-exceptions` subtractions.

   The lifted predicate takes a map that must contain at least
   `:leaf-value`.
"
  [pred & subtractions]
  (if (lifted/already-lifted? pred)
    pred
    (apply lift-pred-map (lifted/pred->about-pred pred) subtractions)))
