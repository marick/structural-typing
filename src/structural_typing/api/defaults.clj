(ns structural-typing.api.defaults
  "User-visible default behaviors, plus the functions used to construct them."
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

(defn- friendly-path-component [component]
  (cond (contains? path/friendly-path-components component)
        (path/friendly-path-components component)

        :else
        (str component)))

(defn- perhaps-indexed [{:keys [leaf-index leaf-count]} string-so-far]
  (if (and leaf-index leaf-count (> leaf-count 1)) ; existence checks simplify tests
    (format "%s[%s]" string-so-far leaf-index)
    string-so-far))

(defn- perhaps-simplified-path [components]
  (if (= 1 (count components))
    (first components)
    (cl-format nil "[~{~A~^ ~}]" components)))

(defn friendly-path [{:keys [path] :as explanation}]
  (->> path
       (map friendly-path-component)
       perhaps-simplified-path
       (perhaps-indexed explanation)))

(defn default-error-explainer [{:keys [predicate-string leaf-value] :as explanation}]
  (format "%s should be `%s`; it is `%s`"
          (friendly-path explanation)
          predicate-string
          (pr-str leaf-value)))

