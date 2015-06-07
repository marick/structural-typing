(ns structural-typing.customize
  (:refer-clojure :exclude [instance?])
  (:require [clojure.string :as str]
            [bouncer.core :as b]
            [structural-typing.validators :as v]))

(defn default-bouncer-map-adapter [error-map checked-map]
  (flatten (vals error-map)))

(defn default-error-string-producer [{path :path, value :value optional-message-arg :message
                                      {default-message-format :default-message-format} :metadata
                                      :as kvs}]
  (let [handler (or optional-message-arg default-message-format
                    "configuration error: no message format")]
    (if (fn? handler)
      (handler kvs)
      (format handler
              (pr-str (if (= 1 (count path)) (first path) path))
              (pr-str value)))))

