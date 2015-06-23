(ns structural-typing.api.defaults
  "Top-level default behaviors, plus the functions used to construct them."
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :as str]
            [clojure.repl :as repl])
  (:require [structural-typing.api.path :as path]))

(declare default-error-explainer)

(defn friendly-function-name [f]
  (cond (var? f)
        (str (:name (meta f)))

        (fn? f)
        (let [basename (-> (str f)
                             repl/demunge
                             (str/split #"/")
                             last
                             (str/split #"@")
                             first
                             (str/split #"--[0-9]+$")
                             first)]
          (if (= basename "fn") "your custom predicate" basename))

        :else
        (str f)))

(defn friendly-path-component [component]
  (cond (contains? path/friendly-path-components component)
        (path/friendly-path-components component)

        :else
        (str component)))

(defn friendly-path [{:keys [path leaf-index leaf-count] :as explanation}]
  (let [tokens (map friendly-path-component path)
        full-path (if (= 1 (count tokens))
                    (first tokens)
                    (cl-format nil "[~{~A~^ ~}]" tokens))]
    (if (and leaf-index leaf-count (> leaf-count 1)) ; existence checks simplify tests
      (format "%s[%s]" full-path leaf-index)
      full-path)))
             

(defn default-error-explainer [{:keys [predicate-string leaf-value] :as explanation}]
  (format "%s should be `%s`; it is `%s`"
          (friendly-path explanation)
          predicate-string
          (pr-str leaf-value)))

