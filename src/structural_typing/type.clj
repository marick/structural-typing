(ns structural-typing.type
  "Structural types, loosely inspired by Elm's way of looking at [records](http://elm-lang.org/learn/Records.elm).
"
  (:use structural-typing.clojure.core)
  (:require [structural-typing.assist.type-repo :as repo]
            [structural-typing.guts.compile.compile :as compile]
            [structural-typing.global-type :as global-type]
            [such.readable :as readable]
            [such.ns :as ns]
            [such.symbols :as sym]
            [such.metadata :as meta]
            [such.immigration :as immigrate]
            [defprecated.core :as depr])
  (:require structural-typing.assist.special-words
            structural-typing.assist.defaults))

(import-all-vars structural-typing.assist.special-words)

(import-vars [structural-typing.assist.type-repo
                        empty-type-repo
                        replace-error-handler
                        replace-success-handler])

(import-vars [structural-typing.assist.defaults
                        throwing-error-handler
                        default-error-handler
                        default-success-handler])


;;;; 

(defn- produce-type [type-repo one-or-more]
  (let [[signifiers condensed-descriptions]
        (bifurcate repo/valid-type-signifier? (force-vector one-or-more))

        compiled-named (mapv #(repo/get-compiled-type type-repo %) signifiers)
        compiled-unnamed (->> condensed-descriptions
                              (repo/canonicalize type-repo)
                              compile/compile-type)]
    (conj compiled-named compiled-unnamed)))


(defn- all-oopsies [compiled-type candidate]
  (reduce (fn [so-far checker]
            (into so-far (checker candidate)))
          []
          compiled-type))

(defn- respond-to-results [type-repo candidate oopsies]
  (if (empty? oopsies)
    ((repo/the-success-handler type-repo) candidate)
    (->> oopsies
         ((repo/the-error-handler type-repo)))))


(defn built-like
  "`type-shorthand` is either a type-signifier (typically a keyword like `:Point`), a condensed
   type description (like `(requires :x :y)`), or a vector containing either or both.
   `built-like` checks the `candidate` against the shorthand. 

   By default, `built-like` will either return the `candidate` or, if the candidate doesn't match the shorthand,
   print an error message and return `nil`. 
   If the `type-repo` is omitted, the global one is used.
   
       (type/built-like :Point {:x 1 :y 2})
       (type/built-like [:Colorful :Point] {:x 1, :y 2, :color \"red\"})
       (type/built-like [:Colorful (requires :x :y)] {:x 1, :y 2, :color \"red\"})

   Types are defined with [[named]] or [[type!]]. Default behavior is changed with
   [[replace-success-handler]], [[replace-error-handler]], [[on-success!]], and [[on-error!]].
"
  ([type-repo type-shorthand candidate]
     (->> candidate
          (all-oopsies (produce-type type-repo type-shorthand))
          (respond-to-results type-repo candidate)))
  ([type-shorthand candidate]
     (built-like @global-type/repo type-shorthand candidate)))

(defn <>built-like
  "The same as [[built-like]] but intended to be used in `->`
   pipelines. Consequently, the `candidate` argument comes first.
   
         (-> emr-patient
             augment           (<>built-like [:Decidable Patient])
             audit
             decide
             schedule)
   
   (The `<>` is intended to remind you of
   [swiss arrows](https://github.com/rplevy/swiss-arrows).)"
  ([candidate type-repo type-shorthand]
     (built-like type-repo type-shorthand candidate))
  ([candidate type-shorthand]
     (built-like type-shorthand candidate)))

(defn all-built-like
  "Check each of the `candidates`. Perform the `type-repo`'s error behavior if *any* of
   the candidates fail. Otherwise, return the original `candidates`.
   
       (some->> (all-built-like :Point [{:x 1, :y 2}
                                        {:why \"so serious?\"}])
                (map process-points))

   Error messages will include the index of the structure that failed.
"
  ([type-repo type-shorthand candidates]
     (let [compiled-type (produce-type type-repo type-shorthand)
           oopsie-chunks (map-indexed (fn [index candidate]
                                        (->> (all-oopsies compiled-type candidate)
                                             (map (fn [oopsie]
                                                    (assoc oopsie :path
                                                           (into (vector index) (:path oopsie)))))))
                                      candidates)]
       (respond-to-results type-repo candidates (apply concat oopsie-chunks))))
  ([type-shorthand candidates]
     (all-built-like @global-type/repo type-shorthand candidates)))

(defn <>all-built-like
  "The same as [[all-built-like]] but intended to be used in `->`
   pipelines. Consequently, the `candidates` argument comes first.
   
         (-> emr-patients
             augment           (<>all-built-like [:Decidable Patient])
             audit
             decide
             schedule)
   
   (The `<>` is intended to remind you of
   [swiss arrows](https://github.com/rplevy/swiss-arrows).)"
  ([candidates type-repo type-shorthand]
     (all-built-like type-repo type-shorthand candidates))
  ([candidates type-shorthand]
     (<>all-built-like candidates @global-type/repo type-shorthand)))
  
(defn named
  "Define `type-signifier` inside the `type-repo` in terms of the
  `condensed-type-descriptions`.
  
  Returns the augmented `type-repo`. See also [[named!]].
"
  [type-repo type-signifier & condensed-type-descriptions]
     (repo/hold-type type-repo type-signifier condensed-type-descriptions))

(defn built-like? 
  "`type-shorthand` is either a type-signifier (typically a keyword like `:Point`), a condensed
   type description (like `(requires :x :y)`), or a vector containing either or both.

   Returns `true` iff the `candidate` structure matches everything in the shorthand.

   With three arguments, the check is against the `type-repo`. If `type-repo` is
   omitted, the global repo is used.
   
       (type/built-like? :Point candidate)
       (type/built-like? [:Colorful :Point] candidate)
"
  ([type-repo type-shorthand candidate]
     (empty? (all-oopsies (produce-type type-repo type-shorthand) candidate)))
  ([type-shorthand candidate]
     (built-like? @global-type/repo type-shorthand candidate)))

(depr/defn described-by? 
  "Use [[built-like?]] instead."
  {:deprecated {:in "0.13.0"
                :use-instead built-like?}}
  ([type-repo type-shorthand candidate]
     (built-like? type-repo type-shorthand candidate))
  ([type-shorthand candidate]
     (described-by? @global-type/repo type-shorthand candidate)))

(defn origin
  "Returns the original condensed type description associated with the `type-signifier`. 
   Uses the global type repo if none is given.
   
   The result is not a string, but rather a structure tweaked to
   look nice either at the repl or as the output from `pprint`. However,
   that means it's not a real type description; you can't feed it back
   to [[named]] or [[type!]].
"
  ([type-repo type-signifier]
     (repo/origin type-repo type-signifier))
  ([type-signifier]
     (repo/origin @global-type/repo type-signifier)))

(defn description
  "Returns the canonical (expanded) description of the `type-signifier`.
   Uses the global type repo if none is given. 
   
   The result is not a string, but rather a structure tweaked to
   look nice either at the repl or as the output from `pprint`. However,
   that means it's not a real type description; you can't feed it back
   to [[named]] or [[type!]]."
  ([type-repo type-signifier]
     (readable/value (repo/description type-repo type-signifier)))
  ([type-signifier]
     (description @global-type/repo type-signifier)))


(defn ^:no-doc set-doc-strings [var-syms]
  (doseq [sym var-syms]
    (let [v (ns/+find-var sym)
          doc (meta/get (ns/+find-var 'structural-typing.type sym) :doc)]
    (alter-meta! v assoc :doc doc))))


(def ^:private ^:no-doc standard-symbols '[built-like all-built-like <>built-like <>all-built-like
                                           built-like?])

(defmacro ensure-standard-functions
  "Suppose you are creating a type repo inside a namespace, as is done in
   the [logging example](https://github.com/marick/structural-typing/blob/master/examples/timbre_define_1.clj). You'd like that namespace to provide functions that use that type repo
   without having to constantly refer to it:

       (my.types/built-like? :Point xy)
       ;; instead of:
       (my.types/built-like? my.types/type-repo :Point xy)

  This function takes a type repo and creates type-repo-specific functions for you.

       (in-ns 'my.types)
       (type/ensure-standard-functions type-repo)
"
  [type-repo-sym]
  `(do
     (doseq [s# '~standard-symbols] (ns-unmap *ns* s#))

     (def ~'built-like (partial structural-typing.type/built-like ~type-repo-sym))
     (def ~'all-built-like (partial structural-typing.type/all-built-like ~type-repo-sym))
     (def ~'<>built-like #(structural-typing.type/<>built-like %1 ~type-repo-sym %2))
     (def ~'<>all-built-like #(structural-typing.type/<>all-built-like %1 ~type-repo-sym %2))
     (def ~'built-like? (partial built-like? ~type-repo-sym))

     (set-doc-strings '~standard-symbols)))
