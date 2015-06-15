(ns structural-typing.f-bouncer-errors
  (:require [structural-typing.bouncer-errors :as subject])
  (:use midje.sweet))


(fact "flatten-error-map makes nesting easier to deal with"
  (subject/flatten-error-map nil) => empty?
  (subject/flatten-error-map {}) => empty?
  (subject/flatten-error-map {:a ["a message" "a message 2"]}) => ["a message" "a message 2"]
  (subject/flatten-error-map {:a ["a message"]
                              :point {:x ["x wrong"]
                                      :y ["y wrong"]}
                              :deep {:er {:still ["still wrong"]}}})
  => (just "a message" "x wrong" "y wrong" "still wrong" :in-any-order))


                             


(fact prepend-bouncer-result-path
  (let [bouncer-diagnostics {:x [{:path [:x] :message "1"}
                                 {:path [:x] :message "2"}]
                             :y [{:path [:y] :message "3"}]}
        updated-path {:x [{:path [888 :x] :message "1"}
                                 {:path [888 :x] :message "2"}]
                             :y [{:path [888 :y] :message "3"}]}]
    (subject/prepend-bouncer-result-path
     [888]
     [bouncer-diagnostics {:bouncer.core/errors bouncer-diagnostics :x 1 :y 2}])
    => [updated-path {:bouncer.core/errors updated-path :x 1 :y 2}]))
    
                             
