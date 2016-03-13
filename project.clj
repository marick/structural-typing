(defproject marick/structural-typing "2.0-beta2"
  :description "Define types by giving descriptions (perhaps incomplete) of how they're built.
               \"...by how they're built\" means applying arbitrary predicates (especially ones
               about existence) to parts of structures.

               The top-level namespaces contain what you need to use the library, including
               simpler customizations.

               The .assist namespaces are useful for more in-depth customizations."
  :url "https://github.com/marick/structural-typing"
  :pedantic? :warn
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/mit-license.php"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [marick/suchwow "5.1.0"]
                 [swiss-arrows "1.0.0"]
                 [defprecated "0.1.3"]
                 [ordered "1.3.2" :exclusions [org.clojure/clojure]]
                 [org.clojure/math.combinatorics "0.1.1"]
                 [com.rpl/specter "0.9.2" :exclusions [org.clojure/clojure org.clojure/clojurescript]]]

  :repl-options {:init (do (require 'structural-typing.doc)
                           (such.doc/apis))}

  :profiles {:dev {:dependencies [[midje "1.8.3" :exclusions [org.clojure/clojure]]
                                  [org.blancas/morph "0.3.0" :exclusions [org.clojure/clojure]]
                                  [com.taoensso/timbre "4.3.1" :exclusions [org.clojure/clojure]]
                                  [org.clojure/math.numeric-tower "0.0.4"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             }

  :test-paths ["test" "examples"]

  :plugins [[lein-midje "3.2"]
            [codox "0.8.11"]]

  :codox {:src-dir-uri "https://github.com/marick/structural-typing/blob/master/"
          :src-linenum-anchor-prefix "L"
          :output-dir "/var/tmp/structural-typing-doc"
          :defaults {:doc/format :markdown}}

  :aliases {"compatibility" ["with-profile" "+1.7:+1.8" "midje" ":config" ".compatibility-test-config"]
            "travis" ["with-profile" "+1.7:+1.8" "midje"]}

  ;; For Clojure snapshots
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
)
