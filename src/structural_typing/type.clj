(ns structural-typing.type
  "Structural types, loosely inspired by Elm's way of looking at [records](http://elm-lang.org/learn/Records.elm).
"
  (:require [structural-typing.surface.type-repo :as repo]
            [structural-typing.global-type :as global-type]
            [such.readable :as readable]
            [such.immigration :as immigrate]))

(immigrate/import-vars [structural-typing.guts.preds.required-key
                        required-key])

(immigrate/import-vars [structural-typing.guts.preds.annotated
                        show-as
                        explain-with])

(immigrate/import-vars [structural-typing.surface.type-repo
                        empty-type-repo
                        replace-error-handler
                        replace-success-handler])

(immigrate/import-vars [structural-typing.guts.paths.elements ALL RANGE]
                       [structural-typing.guts.paths.substituting includes]
                       [structural-typing.guts.paths.readable requires forks])

(immigrate/import-vars [structural-typing.surface.defaults
                        throwing-error-handler
                        default-error-handler
                        default-success-handler])


;;;; 

(defn- all-oopsies [type-repo one-or-more candidate]
  (let [signifiers (if (sequential? one-or-more) one-or-more (vector one-or-more))]
    (mapcat #(repo/check-type type-repo % candidate) signifiers)))

(defn checked
  "Check the `candidate` collection against the previously-defined type named by `type-signifier` in
  the given `type-repo`. If the `type-repo` is omitted, the global one is used.
   
       (type/checked :Point {:x 1 :y 2})

   To check if a candidate matches each of a set of types, wrap them in a vector:

       (type/checked [:Colorful :Point] {:x 1, :y 2, :color \"red\"})
   
   Types are defined with [[named]] or [[type!]]. By default, `checked` returns
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
     (checked @global-type/repo type-signifier candidate)))


(defn named 
  "Define the type `type-signifier` as being a map or record containing all of the
   given `type-descriptions` (which may a vector of required keys or paths
   or a (potentially nested) map from keys/paths to predicates or vectors of predicates.

  Returns the augmented `type-repo`. See also [[named!]].
"
  ([type-repo type-signifier & type-descriptions]
     (repo/hold-type type-repo type-signifier type-descriptions)))

  
(defn described-by? 
  "Return `true` iff the `candidate` structure typechecks against
   the type named `type-signifier`. `type-signifier` may also be a vector
   of types, in which case the `candidate` must typecheck against each of them. 

   With three arguments, the check is against the `type-repo`. If `type-repo` is
   omitted, the global repo is used.
   
       (type/described-by? :Point candidate)
       (type/described-by? [:Colorful :Point] candidate)
"
  ([type-repo type-signifier candidate]
     (empty? (all-oopsies type-repo type-signifier candidate)))
  ([type-signifier candidate]
     (described-by? @global-type/repo type-signifier candidate)))

(defn origin
  "Returns the original description of the `type-signifier` (a sequence of vectors and maps). 
   Uses the global type repo if none is given."
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
;;   (checked-internal own-types :type-repo type-repo)
;;   (assoc-in type-repo [:coercions type-signifier] f))

;; (defn coerced
;;   "Coerce the map or record `candidate` into the type named `type-signifier` in the `type-repo`
;;    and check the result with [[checked]]. The coerced version of `candidate` is returned.
   
;;    If the type repo is omitted, the global type repo is used.
;;    Coercions are defined with [[coercion]] or [[coercion!]].
   
;;         (some-> (coerce :user-v2 legacy-json)
;;                 (update-in [:stats :logins] inc))

;;    If `type-signifier` hasn't been defined (via [[name]] or [[named!]]), the final
;;    call to `checked` is omitted.
;; "
;;   ([type-repo type-signifier candidate]
;;      (checked-internal own-types :type-repo type-repo)
;;      (let [coercer (get-in type-repo [:coercions type-signifier] identity)]
;;        (->> (coercer candidate)
;;             (checked-internal type-repo type-signifier))))
;;   ([type-signifier candidate]
;;      (coerced @stages/global-type-repo type-signifier candidate)))

