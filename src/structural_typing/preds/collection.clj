(ns structural-typing.preds.collection)


(comment


  (diff actual {:type-check fn
                :same-order? true
                :extras-allowed? true
                :missing-allowed? true
                :comparison [:strict-equality
                             :midje-like-single-level
                             :midje-like-recursive]})





  ;; (collection/exactly [1, 2 3] :strict-equality true
  ;; (collection/at-least [1, 2 3],
  ;; (collection/at-most [1, 2 3],



  )
