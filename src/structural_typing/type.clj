(ns structural-typing.type
  ""
  (:refer-clojure :exclude [instance?])
  (:require [clojure.string :as str]
            [bouncer.core :as b]
            [bouncer.validators :as v]
            [such.immigration :as immigrate]))

(immigrate/selection 'bouncer.validators '[required])

;;; About our type

(defn- default-formatter [error-map checked-map]
  (flatten (vals error-map)))

(defn- default-failure-handler [msgs]
  (doseq [s msgs]
    (println s))
  nil)

(defn throwing-failure-handler [messages]
  (throw (new Exception (str/join "\n" messages))))

(def ^:private default-success-handler identity)

(defn- formatter [type-repo]
  (get type-repo :formatter default-formatter))
(defn- failure-handler [type-repo]
  (get type-repo :failure-handler default-failure-handler))
(defn- success-handler [type-repo]
  (get type-repo :success-handler default-success-handler))

(def empty-type-repo {:failure-handler default-failure-handler
                      :success-handler default-success-handler
                      :formatter default-formatter})

(declare ^:private own-types)

;;; Non-side-effecting API

(declare checked global-type-repo)

(defn- named-internal [type-repo name keys]
  (let [validator-map (reduce (fn [so-far k] (assoc so-far k v/required))
                              {}
                              keys)]
    (assoc-in type-repo [:validators name] validator-map)))

(defn- checked-internal [type-repo name kvs]
  (let [[errors actual] (b/validate kvs (get-in type-repo [:validators name]))]
    (if (empty? errors)
      ((success-handler type-repo) kvs)
      (-> ( (formatter type-repo) errors (dissoc actual :bouncer.core/errors))
          ((failure-handler type-repo))))))

(defn named [type-repo name keys]
  (checked-internal own-types :type-repo type-repo)
  (named-internal type-repo name keys))

(defn checked
  ([type-repo name kvs]
     (checked-internal own-types :type-repo type-repo)
     (checked-internal type-repo name kvs))
  ([name kvs]
     (checked @global-type-repo name kvs)))

(defn instance? 
  ([type-repo name kvs]
     (checked (assoc type-repo
                     :failure-handler (constantly false) 
                     :success-handler (constantly true))
              name 
              kvs))
  ([name kvs]
     (instance? @global-type-repo name kvs)))

(defn coercion [type-repo name f]
  (checked-internal own-types :type-repo type-repo)
  (assoc-in type-repo [:coercions name] f))

(defn coerce
  ([type-repo name kvs]
     (checked-internal own-types :type-repo type-repo)
     (let [coercer (get-in type-repo [:coercions name] identity)]
       (->> (coercer kvs)
            (checked-internal type-repo name))))
  ([name kvs]
     (coerce @global-type-repo name kvs)))


;;; Own types

(def own-types (-> empty-type-repo
                   (assoc :failure-handler throwing-failure-handler)
                   (named-internal :type-repo [:success-handler :failure-handler :formatter])))

;;; Side-effecting API

(def ^:private global-type-repo)

(defn ^:no-doc start-over! []
  (alter-var-root #'global-type-repo (fn [_] (atom empty-type-repo))))
(start-over!)

(defn set-success-handler! [f]
  (swap! global-type-repo assoc :success-handler f))
(defn set-failure-handler! [f]
  (swap! global-type-repo assoc :failure-handler f))
(defn set-formatter! [f]
  (swap! global-type-repo assoc :formatter f))
  
(defn named! [name keys]
  (swap! global-type-repo named name keys))

(defn coercion! [name f]
  (swap! global-type-repo coercion name f))

