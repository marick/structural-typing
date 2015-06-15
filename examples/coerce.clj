(ns coerce
  "You can coerce a map into a particular type"
  (:require [structural-typing.type :as type]
            [clojure.set :as set]
            [structural-typing.testutil.accumulator :as accumulator])
  (:use midje.sweet))

(namespace-state-changes (before :facts (accumulator/reset!)))

;; We start with a test support repo. You'd normally use `type/empty-type-repo`.
(def type-repo (-> accumulator/type-repo
                   (type/named :some-type [:a :b])
                   (type/coercion :some-type (fn [from] (set/rename-keys from {:aaaa :a})))))

(fact "basic use"
  (type/coerced type-repo :some-type {:aaaa 1, :b 2}) => {:a 1, :b 2})


(def repo-with-bad-coercion (-> accumulator/type-repo
                                (type/named :broken [:a :b])
                                ;; This coercion renames required key `:a`, thus ensuring
                                ;; that every map will fail.
                                (type/coercion :broken
                                               (fn [from] (set/rename-keys from {:a :aa})))))

(fact "the result is itself checked"
  (type/coerced repo-with-bad-coercion :broken {:a 1, :b 2}) => :failure-handler-called
  (accumulator/messages) => (just #"a must be present"))


(def repo-that-only-coerces (-> type/empty-type-repo
                                (type/coercion :some-coercion
                                               (fn [from] (set/rename-keys from {:a :bbb})))))
  
(fact "note that it's OK for there to be no type check for a coercion result."
  (type/coerced repo-that-only-coerces :some-coercion {:a 1, :b 2}) => {:bbb 1, :b 2})

