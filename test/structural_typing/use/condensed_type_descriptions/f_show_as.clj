(ns structural-typing.use.condensed-type-descriptions.f-show-as
  (:use midje.sweet
        structural-typing.type
        structural-typing.global-type
        structural-typing.clojure.core
        structural-typing.assist.testutil)
  (:refer-clojure :except [any?]))


(start-over!)


(fact "without `show-as`"
  (def good-password? (partial re-find #"\d"))
  (type! :Account {:password good-password?})
  
  (check-for-explanations :Account {:password "fool"})
  => [(err:shouldbe :password "<custom-predicate>" "\"fool\"")])

(fact "a typical use of show-as"
  (type! :Address 
         {:name (show-as "short string" #(<= % 10))})

  (check-for-explanations :Address {:name "llllllllllllllllll"})
  => [(err:shouldbe :name "short string" "\"llllllllllllllllll\"")])

(start-over!)
