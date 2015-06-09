(ns structural-typing.validators
  "Validators. These differ from Bouncer validators in that (1) they default to optional, and
   (2) the messages include the failing value."
  (:require [bouncer.validators :as v]))

(defmacro defoptional [name doc message-format & body]
  `(do
     (v/defvalidator ~name {:optional true
                            :default-message-format ~message-format} ~@body)
     (alter-meta! (var ~name) assoc :doc ~doc)))

(v/defvalidator ^{:doc "Fails if key is missing or its value is `nil`."} required
  {:default-message-format "%s must be present and non-nil"}
  [v]
  (not (nil? v)))

(defoptional number
  "Validates against optional `number?`"
  "%s is `%s`, which is not a number"
  [maybe-a-number]
  (number? maybe-a-number))
