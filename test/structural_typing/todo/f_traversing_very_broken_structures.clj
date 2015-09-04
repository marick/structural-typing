(ns structural-typing.todo.f-traversing-very-broken-structures
  (:use structural-typing.type
        structural-typing.global-type)
  (:use midje.sweet structural-typing.assist.testutil))

(start-over!)

(future-fact "traversing integers and the like"
  (type! :KeysAlone :x :y)
  (check-for-explanations :KeysAlone 1) =future=> "something about being unable to traverse path"
  )

(start-over!)
