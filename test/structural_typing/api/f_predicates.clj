(ns structural-typing.api.f-predicates
  (:require [structural-typing.api.predicates :as subject]
            [structural-typing.mechanics.m-lifting-predicates :refer [lift]])
  (:require [blancas.morph.monads :as e])
  (:use midje.sweet))


;; (fact "note that `message` and `only-when` can be combined"
;;   (let [type-repo (-> accumulator/type-repo
;;                       (type/named :positive-even [:a]
;;                                   {:a [#'integer? (-> even?
;;                                                       (type/only-when pos?)
;;                                                       (type/message "A positive `%s` should be even."))]}))]
;;     (type/checked type-repo :positive-even {}) => :failure-handler-called
;;     (accumulator/messages) => [":a must be present and non-nil"]

;;     (type/checked type-repo :positive-even {:a :not-integer}) => :failure-handler-called
;;     (accumulator/messages) => [":a should be `integer?`; it is `:not-integer`"]

;;     (type/checked type-repo :positive-even {:a 1}) => :failure-handler-called
;;     (accumulator/messages) => ["A positive `:a` should be even."]

;;     (type/checked type-repo :positive-even {:a 2}) => {:a 2}

;;     ;; The guard prevents a failure
;;     (type/checked type-repo :positive-even {:a -1}) => {:a -1}))

;; (fact "just to be squeaky-sure, combine them in the other order"
;;   (let [type-repo (-> accumulator/type-repo
;;                       (type/named :positive-even [:a]
;;                                   {:a [#'integer? (-> even?
;;                                                       (type/message "A positive `%s` should be even.")
;;                                                       (type/only-when pos?))]}))]
;;     (type/checked type-repo :positive-even {}) => :failure-handler-called
;;     (accumulator/messages) => [":a must be present and non-nil"]

;;     (type/checked type-repo :positive-even {:a :not-integer}) => :failure-handler-called
;;     (accumulator/messages) => [":a should be `integer?`; it is `:not-integer`"]

;;     (type/checked type-repo :positive-even {:a 1}) => :failure-handler-called
;;     (accumulator/messages) => ["A positive `:a` should be even."]

;;     (type/checked type-repo :positive-even {:a 2}) => {:a 2}

;;     ;; The guard prevents a failure
;;     (type/checked type-repo :positive-even {:a -1}) => {:a -1}))



(fact member
  (let [lifted (lift (subject/member 1 2 3))
        result (e/run-left (lifted {:leaf-value 8 :path [:x]}))]
    result => (contains {:path [:x]
                         :leaf-value 8})

    ((:error-explainer result) result) => ":x should be a member of (1 2 3); it is `8`"))
