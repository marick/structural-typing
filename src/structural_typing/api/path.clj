(ns structural-typing.api.path
  (:require [com.rpl.specter :as specter]))

(def ^:private friendly-path-components
  {specter/ALL "ALL"})

(defn friendly-path [path]
  (if (= 1 (count path)) (first path) path))
