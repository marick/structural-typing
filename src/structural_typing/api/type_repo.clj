(ns structural-typing.api.type-repo
  (:require [structural-typing.mechanics.m-canonical :as canon]
            [structural-typing.mechanics.m-compile :as compile]
            [structural-typing.api.defaults :as default]
            [structural-typing.frob :as frob]))

(def ^:no-doc global-type-repo)

(defprotocol TypeRepoLike
  (hold-type [self type-signifier type-descriptions])
  (check-type [self type-signifier value])
  (replace-success-handler [self handler])
  (replace-error-handler [self handler]))

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

    (check-type [self type-signifier value]
      (if-let [checker (get-in self [:compiled-types type-signifier])]
        (let [oopsies (checker value)]
          (if (empty? oopsies)
            (success-handler value)
            (error-handler oopsies)))
        (frob/boom "There is no type `%s`" type-signifier)))
    
    (replace-error-handler [self f]
      (assoc self :error-handler f))

    (replace-success-handler [self f]
      (assoc self :success-handler f)))

(def empty-type-repo (->TypeRepo default/default-success-handler default/default-error-handler))
