(ns structural-typing.api.type-repo)

(def ^:no-doc global-type-repo)

(defprotocol TypeRecordLike)

(defrecord TypeRecord []
    TypeRecordLike
  )
  
