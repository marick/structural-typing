(ns structural-typing.type
  "Structural types, loosely inspired by Elm's way of looking at [records](http://elm-lang.org/learn/Records.elm).
"
  (:use structural-typing.clojure.core)
  (:require [structural-typing.assist.type-repo :as repo]
            [structural-typing.guts.compile.compile :as compile]
            [structural-typing.global-type :as global-type]
            [such.readable :as readable]
            [defprecated.core :as depr]))

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

;; This is used to check if an argument to `built-like` is nil. If so, it's not further
;; checked. Another approach would be to inject the following map into all types when
;; they're compiled. However, that would mean that the `T1` in:
;;     (type! :T2 {:a (includes :T1)})
;; ... would not be optional, which would make it different from all other pred-like values.
(def ^:private whole-type-checker (compile/compile-type {[] [not-nil]}))

(defn- produce-type [type-repo one-or-more]
  (let [[signifiers condensed-descriptions]
        (bifurcate repo/valid-type-signifier? (force-vector one-or-more))

        compiled-named (mapv #(repo/get-compiled-type type-repo %) signifiers)
        compiled-unnamed (->> condensed-descriptions
                              (repo/canonicalize type-repo)
                              compile/compile-type)]
    (conj compiled-named compiled-unnamed)))


(defn- all-oopsies [compiled-type candidate]
    (or (seq (whole-type-checker candidate))
        (reduce (fn [so-far checker]
                  (into so-far (checker candidate)))
                []
                compiled-type)))

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

   Note: When the type-repo's error behavior is called, it is passed all the `candidates`. 
   If this function is used, custom error-handlers need to handle both individual candidate
   failures and group-of-candidate failures.
"
  ([type-repo type-shorthand candidate]
     (let [compiled-type (produce-type type-repo type-shorthand)
           oopsies (mapcat #(all-oopsies compiled-type %) candidate)]
       (respond-to-results type-repo candidate oopsies)))
  ([type-shorthand candidate]
     (all-built-like @global-type/repo type-shorthand candidate)))

(defn <>all-built-like
  "The same as [[all-built-like]] but intended to be used in `->`
   pipelines. Consequently, the `candidate` argument comes first.
   
         (-> emr-patients
             augment           (<>all-built-like [:Decidable Patient])
             audit
             decide
             schedule)
   
   (The `<>` is intended to remind you of
   [swiss arrows](https://github.com/rplevy/swiss-arrows).)"
  ([candidate type-repo type-shorthand]
     (all-built-like type-repo type-shorthand candidate))
  ([candidate type-shorthand]
     (<>all-built-like candidate @global-type/repo type-shorthand)))
  
(depr/defn checked
  "Use [[built-like]] instead."
  {:deprecated {:in "0.13.0"}}
  ([type-repo type-shorthand candidate]
     (built-like type-repo type-shorthand candidate))

  ([type-shorthand candidate]
     (checked @global-type/repo type-shorthand candidate)))


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

(defn described-by? 
  "Use [[built-like?]] instead."
  {:deprecated {:in "0.13.0"}}
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



;; (defn coercion 
;;   "Register function `f` as one that can coerce a map or record into 
;;    one that matches type `type-signifier`. The updated `type-repo` is returned.
;;    See also [[coercion!]], which updates the global type repo."
;;   [type-repo type-signifier f]
;;   (built-like-internal own-types :type-repo type-repo)
;;   (assoc-in type-repo [:coercions type-signifier] f))

;; (defn coerced
;;   "Coerce the map or record `candidate` into the type named `type-signifier` in the `type-repo`
;;    and check the result with [[built-like]]. The coerced version of `candidate` is returned.
   
;;    If the type repo is omitted, the global type repo is used.
;;    Coercions are defined with [[coercion]] or [[coercion!]].
   
;;         (some-> (coerce :user-v2 legacy-json)
;;                 (update-in [:stats :logins] inc))

;;    If `type-signifier` hasn't been defined (via [[name]] or [[named!]]), the final
;;    call to `built-like` is omitted.
;; "
;;   ([type-repo type-signifier candidate]
;;      (built-like-internal own-types :type-repo type-repo)
;;      (let [coercer (get-in type-repo [:coercions type-signifier] identity)]
;;        (->> (coercer candidate)
;;             (built-like-internal type-repo type-signifier))))
;;   ([type-signifier candidate]
;;      (coerced @stages/global-type-repo type-signifier candidate)))

