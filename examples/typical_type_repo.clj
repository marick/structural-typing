(ns typical-type-repo)
;;   "Defining a typical type repo"
;;   (:require [structural-typing.type :refer :all
;;                                     :exclude [checked described-by?]
;;                                     :as type])
;;   (:use midje.sweet))


;; (def type-repo (-> empty-type-repo
;;                    (named :Point {:x integer? :y integer?})
;;                    ; ...
;;                    ))

;; (def checked (partial type/checked type-repo))
;; (def described-by? (partial type/described-by? type-repo))

;; ;; Clients now do `(:require [myapp.types :as type] ...)
;; ;; ... and check types like this:
;; ;;
;; ;; (some-> incoming-data
;; ;;         (type/checked :Point)
;; ;;         process-data
;; ;;         ...)



;; (fact "worked"
;;   (checked :Point {:x 1 :y 2}) => {:x 1 :y 2})
