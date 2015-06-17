(ns structural-typing.bouncer-validators
  "A facade over bouncer, decoupling (somewhat) this library from its details"
  (:require [structural-typing.frob :as frob]
            [bouncer.core :as b]))


(defn check [type-repo type-signifier candidate]
  (b/validate (:error-explanation-producer type-repo)
              candidate
              (get-in type-repo [:validators type-signifier])))


