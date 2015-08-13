(ns structural-typing.testutil.shapes
  (:require [structural-typing.guts.shapes.exval :as exval]
            [structural-typing.surface.oopsie :as oopsie]
            [structural-typing.surface.mechanics :as mechanics]
            [such.readable :as readable]))

(defn exval
  ([leaf-value path whole-value]
     (exval/boa leaf-value path whole-value))
  ([leaf-value path]
     (exval leaf-value path (hash-map path leaf-value)))
  ([leaf-value]
     (exval leaf-value [:x])))

(defn explain-lifted [pred exval]
  (oopsie/explanations ((mechanics/lift pred) exval)))

