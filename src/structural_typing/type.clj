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

(defn- all-oopsies [type-repo one-or-more candidate]
  (let [[signifiers condensed-descriptions]
        (bifurcate repo/valid-type-signifier? (force-vector one-or-more))

        compiled-named (mapv #(repo/get-compiled-type type-repo %) signifiers)
        compiled-unnamed (->> condensed-descriptions
                              (repo/canonicalize type-repo)
                              compile/compile-type)]
    (or (seq (whole-type-checker candidate))
        (reduce (fn [so-far checker]
                  (into so-far (checker candidate)))
                []
                (conj compiled-named compiled-unnamed)))))

(defn built-like 
  "Check the `candidate` collection against the previously-defined type named by `type-signifier` in
  the given `type-repo`. If the `type-repo` is omitted, the global one is used.
   
       (type/built-like :Point {:x 1 :y 2})

   To check if a candidate matches each of a set of types, wrap them in a vector:

       (type/built-like [:Colorful :Point] {:x 1, :y 2, :color \"red\"})
   
   Types are defined with [[named]] or [[type!]]. By default, `built-like` returns
   the `candidate` argument if it checks out, `nil` otherwise. Those defaults can be
   changed ([[replace-success-handler]], [[replace-error-handler]], [[on-success!]], [[on-error!]]). 
"
  ([type-repo type-signifier candidate]
     (let [oopsies (all-oopsies type-repo type-signifier candidate)]
       (if (empty? oopsies)
         ((repo/the-success-handler type-repo) candidate)
         (->> oopsies
              ((repo/the-error-handler type-repo))))))

  ([type-signifier candidate]
     (built-like @global-type/repo type-signifier candidate)))

(depr/defn checked
  "Use [[built-like]] instead."
  {:deprecated {:in "0.13.0"}}
  ([type-repo type-signifier candidate]
     (built-like type-repo type-signifier candidate))

  ([type-signifier candidate]
     (checked @global-type/repo type-signifier candidate)))


(defn named 
  "Define the type `type-signifier` as being a map or record containing all of the
   given `type-descriptions` (which may a vector of required keys or paths
   or a (potentially nested) map from keys/paths to predicates or vectors of predicates.

  Returns the augmented `type-repo`. See also [[named!]].
"
  [type-repo type-signifier & type-descriptions]
     (repo/hold-type type-repo type-signifier type-descriptions))

(defn built-like? 
  "Return `true` iff the `candidate` structure matches 
   the type named `type-signifier`. `type-signifier` may also be a vector 
   containing type signifiers or condensed type descriptions. The candidate
   must match each of them.

   With three arguments, the check is against the `type-repo`. If `type-repo` is
   omitted, the global repo is used.
   
       (type/built-like? :Point candidate)
       (type/built-like? [:Colorful :Point] candidate)
"
  ([type-repo type-signifier candidate]
     (empty? (all-oopsies type-repo type-signifier candidate)))
  ([type-signifier candidate]
     (built-like? @global-type/repo type-signifier candidate)))

(defn described-by? 
  "Use [[built-like?]] instead."
  {:deprecated {:in "0.13.0"}}
  ([type-repo type-signifier candidate]
     (built-like? type-repo type-signifier candidate))
  ([type-signifier candidate]
     (described-by? @global-type/repo type-signifier candidate)))

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

