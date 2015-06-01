(ns structural-typing.validators
  "Validators. These differ from Bouncer validators in that (1) they default to optional, and
   (2) the messages include the failing value."
  (:require [bouncer.validators :as v]))

(v/defvalidator required 
  {:default-message-format "%s must be present and non-nil"}
  [v]
  (not (nil? v)))

(v/defvalidator number
  "Validates against `number?`"
  {:default-message-format "%s is `%s`, which is not a number"
   :optional true}
  [maybe-a-number]
  (number? maybe-a-number))
