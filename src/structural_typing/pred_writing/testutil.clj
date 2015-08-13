(ns structural-typing.pred-writing.testutil
  (:require [structural-typing.guts.shapes.exval :as exval]
            [structural-typing.pred-writing.oopsie :as oopsie]
            [structural-typing.pred-writing.lifting :as lifting]
            [such.readable :as readable]))

(defn exval
  ([leaf-value path whole-value]
     (exval/boa leaf-value path whole-value))
  ([leaf-value path]
     (exval leaf-value path (hash-map path leaf-value)))
  ([leaf-value]
     (exval leaf-value [:x])))

(defn explain-lifted [pred exval]
  (oopsie/explanations ((lifting/lift pred) exval)))

