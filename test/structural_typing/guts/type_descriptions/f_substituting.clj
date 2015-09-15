(ns structural-typing.guts.type-descriptions.f-substituting
  (:require [com.rpl.specter :as specter])
  (:require [structural-typing.guts.type-descriptions.substituting :as subject])
  (:require [structural-typing.guts.type-descriptions.elements :refer [ALL RANGE]])
  (:use midje.sweet))

(fact path-will-match-many?
  (subject/path-will-match-many? [:a :b]) => false
  (subject/path-will-match-many? [:a ALL :b]) => true)

(fact replace-with-indices
  (fact "ALL needn't worry about offsets"
    (subject/replace-with-indices [ALL ALL] [17 3]) => [17 3]
    (subject/replace-with-indices [:a ALL :b ALL] [17 3]) => [:a 17 :b 3])
  (fact "... and, as it happens, RANGE needn't either"
    (subject/replace-with-indices [(RANGE 3 100) ALL] [17 3]) => [17 3]
    (subject/replace-with-indices [:a ALL :b (RANGE 1 100)] [17 3]) => [:a 17 :b 3]))
    
    
