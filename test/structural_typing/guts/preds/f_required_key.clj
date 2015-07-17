(ns structural-typing.guts.preds.required-key
  (:require [structural-typing.guts.mechanics.required-key :as subject]
            [structural-typing.guts.mechanics.m-preds :as pred]
            [structural-typing.surface.defaults :as default])
  (:require [com.rpl.specter :refer [ALL]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))

