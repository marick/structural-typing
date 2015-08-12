(ns structural-typing.guts.shapes.f-exval
  (:require [structural-typing.guts.shapes.exval :as subject])
  (:use midje.sweet))


(fact "creating exvals"
  (subject/exvals-for-leafs ..path.. ..whole.. [..one.. ..two..])
  => (just {:path ..path.. :whole-value ..whole.. :leaf-value ..one..}
           {:path ..path.. :whole-value ..whole.. :leaf-value ..two..}))

