(ns structural-typing.f-bouncer-errors
  (:require [structural-typing.bouncer-errors :as subject]
            [bouncer.core :as b]
            [bouncer.validators :as v])
  (:use midje.sweet))


(fact "simplifying message formats"
  (let [holder (atom [])
        add #(do (swap! holder conj %) nil)
        only #(first @holder)]
    (against-background [(before :facts (reset! holder []))]
      
      (fact "simple bouncer form"
        (b/validate add {} {:a v/required})
        (only) => (contains {:path [:a]
                             :value nil
                             :args nil
                             :metadata (contains {:validator v/required
                                                  :optional false
                                                  :default-message-format "%s must be present"})
                             :message nil})

        (subject/simplify (only)) => {:path [:a]
                                      :value nil
                                      :predicate-args nil
                                      :message "%s must be present"})
                                      
                                      
      
      (fact "Form with predicate"
        (b/validate add {:a 3} {:a even?})
        (only) => (contains {:path [:a]
                             :value 3
                             :args nil
                             :metadata (contains {;; :validator even? ; dunno why this doesn't work
                                                  :default-message-format "Custom validation failed for %s"
                                                  })
                             :message nil}))
      
        (subject/simplify (only)) => {:path [:a]
                                      :value 3
                                      :predicate-args nil
                                      :message "Custom validation failed for %s"}
                                      
                                      
      
      (fact "Form with args"
        (b/validate add {:a 3} {:a [[v/member [1 2]]]})
        (only) => (contains {:path [:a]
                             :value 3
                             :args [[1 2]]
                             :metadata (contains {:validator :bouncer.validators/member
                                                  :optional false
                                                  :default-message-format "%s must be one of the values in the list"
                                                  })
                             :message nil}))
        (subject/simplify (only)) => {:path [:a]
                                      :value 3
                                      :predicate-args [[1 2]]
                                      :message "%s must be one of the values in the list"}
      
      (fact "Form with message"
        (b/validate add {:a 3} {:a [[even? :message "%s even"]]})
        (only) => (contains {:path [:a]
                             :value 3
                             :args nil
                             :metadata (contains {
                                                  :default-message-format "Custom validation failed for %s"
                                                  })
                             :message "%s even"}))
        (subject/simplify (only)) => {:path [:a]
                                      :value 3
                                      :predicate-args nil
                                      :message "%s even"}
      )))

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
    
                             
