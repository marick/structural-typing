(ns structural-typing.api.defaults
  "User-visible default behaviors, plus the functions used to construct them."
  (:require [clojure.pprint :refer [cl-format]]
            [clojure.string :as str]
            [clojure.repl :as repl])
  (:require [structural-typing.api.path :as path]))

(declare default-predicate-explainer default-success-handler default-explanation-handler)

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

(defn friendly-path [{:keys [path] :as oopsie}]
  (->> path
       (map friendly-path-component)
       perhaps-simplified-path
       (perhaps-indexed oopsie)))

(defn explanation [oopsie]
  ((:predicate-explainer oopsie) oopsie))

(defn explanations [oopsies]
  (map explanation oopsies))


;;; 

(defn default-predicate-explainer [{:keys [predicate-string leaf-value] :as oopsie}]
  (format "%s should be `%s`; it is `%s`"
          (friendly-path oopsie)
          predicate-string
          (pr-str leaf-value)))

(def default-success-handler 
  "The default success handler just returns the original value that was checked."
  identity)

(defn default-error-handler
  "This error handler prints each error's explanation on a separate line. It returns
   `nil`, allowing constructs like this:
   
        (some-> (type/checked :frobnoz x)
                (assoc :goodness true)
                ...)
"
  [oopsies]
  (doseq [s (explanations oopsies)]
    (println s))
  nil)


(defn throwing-failure-handler 
  "In contrast to the default error handler, this one throws a
   `java.lang.Exception` whose message is the concatenation of the
   type-mismatch messages.
   
   To make all type mismatches throw failures, do this:
   
          (type/set-error-handler! type/throwing-failure-handler)
"
  [oopsies]
  (throw (new Exception (str/join "\n" (explanations oopsies)))))



