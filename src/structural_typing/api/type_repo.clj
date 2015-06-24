(ns structural-typing.api.type-repo
  (:require [structural-typing.mechanics.m-canonical :as canon]
            [structural-typing.mechanics.m-compile :as compile]
            [structural-typing.api.defaults :as default]
            [structural-typing.frob :as frob]))

(defprotocol TypeRepoLike
  (hold-type [self type-signifier type-descriptions])
  (oopsies [self type-signifier candidate])
  (replace-success-handler [self handler])
  (replace-error-handler [self handler])
  (success-handler [self])
  (error-handler [self]))

(defrecord TypeRepo [success-handler error-handler]
    TypeRepoLike
    (hold-type [self type-signifier type-descriptions]
      (let [canonicalized (apply canon/canonicalize
                                 (:canonicalized-type-descriptions self)
                                 type-descriptions)
            compiled (compile/compile-type canonicalized)]
        (-> self 
            (assoc-in [:original-type-descriptions type-signifier] type-descriptions)
            (assoc-in [:canonicalized-type-descriptions type-signifier] canonicalized)
            (assoc-in [:compiled-types type-signifier] compiled))))

    (oopsies [self type-signifier candidate]
      (if-let [checker (get-in self [:compiled-types type-signifier])]
        (checker candidate)
        (frob/boom "There is no type `%s`" type-signifier)))
    
    (replace-error-handler [self f]
      (assoc self :error-handler f))

    (replace-success-handler [self f]
      (assoc self :success-handler f))

    (error-handler [self] (:error-handler self))
    (success-handler [self] (:success-handler self)))

(def empty-type-repo (->TypeRepo default/default-success-handler default/default-error-handler))

