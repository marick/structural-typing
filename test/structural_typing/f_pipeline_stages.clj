(ns structural-typing.f-pipeline-stages
  (:require [structural-typing.pipeline-stages :as subject]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

(fact "the default bouncer map adapter flattens the error maps and ignores the source map"
  (subject/default-map-adapter {:a ["msg1" "msg2"] :b ["msg3"]} ..ignored..)
  => (just "msg1" "msg2" "msg3" :in-any-order))

(facts "about default message formatting"
  (fact "a typical call formats keys and values, uses default-message-format"
    (subject/default-error-explanation-producer {:path [:a]
                                            :value "wrong"
                                            :metadata {:default-message-format "%s - %s"}})
    => ":a - \"wrong\"")

  (fact "a non-singular path is printed as an array"
    (subject/default-error-explanation-producer {:path [:a, :b]
                                            :value "wrong"
                                            :metadata {:default-message-format "%s - %s"}})
    => "[:a :b] - \"wrong\"")

  (fact "a message-format overrides the default"
    (subject/default-error-explanation-producer {:path [:a, :b]
                                            :value "wrong"
                                            :metadata {:default-message-format "%s - %s"}
                                            :message "%s derp %s"})
    => "[:a :b] derp \"wrong\"")

  (fact "a single format argument is allowed in a message"
    (subject/default-error-explanation-producer {:path [:a]
                                            :value "wrong"
                                            :metadata {:default-message-format "%s must be present"}})
    => ":a must be present")

  (fact "the default message format can be a function that takes the bouncer map"
    (subject/default-error-explanation-producer {:path ["a"]
                                            :value 3
                                            :metadata {:default-message-format
                                                       #(format "%s/%s" (:path %) (inc (:value %)))}})
    => "[\"a\"]/4")
  
  (fact "... as can be the given message format (which is given raw path and value)"
    (subject/default-error-explanation-producer {:path ["a"]
                                            :value 3
                                            :metadata {:default-message-format
                                                       #(format "%s/%s" (:path %) (inc (:value %)))}})
    => "[\"a\"]/4")
  
  )

(future-fact "throwing failure handler")
