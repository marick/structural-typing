(ns structural-typing.api.custom
  "Functions useful when overriding default behavior."
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :as str]
            [clojure.repl :as repl])
  (:require [structural-typing.api.path :as path]))

(defn friendly-function-name
  "The argument is probably a function. Produce a string that will help a
   human understand which chunk o' code is being referred to. Can also take
   vars - potentially useful when the raw function is befuddling. For other
   cases, like multimethods, it just punts to `str`.

       (d/friendly-function-name even?) => \"even?\"
       (d/friendly-function-name #'even?) => \"even?\"
"


  [f]
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

(defn friendly-path
  "Convert the path into a string, with Specter components printed nicely and
   an index appended if needed."
  [oopsie]
  (->> oopsie
       :path
       (map friendly-path-component)
       perhaps-simplified-path
       (perhaps-indexed oopsie)))

(defn explanation
  "Convert an [[oopsie]] into a string explaining the error,
   using the `:predicate-explainer` within it."
  [oopsie]
  ((:predicate-explainer oopsie) oopsie))

(defn explanations 
  "Convert a collection of [[oopsies]] into a collection of explanatory strings.
   See [[explanation]]."
  [oopsies]
  (map explanation oopsies))

