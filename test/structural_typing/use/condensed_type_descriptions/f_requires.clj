(ns structural-typing.use.condensed-type-descriptions.f-requires
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil))

(fact "an empty `requires`"
  (type! :X (requires))
  (built-like :X 3) => 3)

(fact "requires can be given either keys or paths"
  (type! :V1 (requires :x [:y :z]))
  (type! :V2 {[:x] [required-key]
              [:y :z] [required-key]})

  (tabular
    (fact
      (let [in {:x 2 :y {:z 1}}]
        (built-like ?version in) => in)
      (check-for-explanations ?version {:x 2 :y {}}) => [(err:required [:y :z])])
    ?version
    :V1
    :V2))
