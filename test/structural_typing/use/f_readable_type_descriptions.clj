(ns structural-typing.use.f-readable-type-descriptions
  (:require [structural-typing.type :as type]
            [structural-typing.preds :as pred])
  (:use midje.sweet
        structural-typing.assist.special-words))

(fact "origin and description"
  (let [origin (list (requires :x [:y :z])
                     {:tag (pred/exactly 'even)
                      :x integer?})
        repo (apply type/named type/empty-type-repo :X origin)]
    (type/origin repo :X) => origin
    (let [result (type/description repo :X)]
      (get result [:x]) => ['required-path 'integer?]
      (get result [:y :z]) => ['required-path]
      ;; Because `(exactly even)` is a functiuon, its name is turned into a symbol.
      ;; That's probably wrong, but it's actually convenient as it prints and pprints
      ;; more nicely.
      (get result [:tag]) => [(symbol "(exactly even)")]
      (with-out-str (clojure.pprint/pprint result)) =>
"{[:x] [required-path integer?],
 [:y :z] [required-path],
 [:tag] [(exactly even)]}
")))

(fact (pr-str (requires :a :b [:c :d])) => "(required :a :b [:c :d])")


