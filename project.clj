(defproject marick/structural-typing "0.12.0"
  :description "Structural typing for Clojure. Generally useful when you want to define predicates to apply in bulk to parts of structures. The top-level namespaces contain what you need to use the library. The .pred-writing namespaces help you tweak its behavior."
  :url "https://github.com/marick/structural-typing"
  :pedantic? :warn
  :license {:name "The Unlicense"
            :url "http://unlicense.org/"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [marick/suchwow "4.0.1"]
                 [com.rpl/specter "0.6.2" :exclusions [org.clojure/clojure]]]

  :repl-options {:init (do (require 'structural-typing.doc)
                           (such.doc/apis))}

  :profiles {:dev {:dependencies [[midje "1.8-alpha1" :exclusions [org.clojure/clojure]]
                                  [org.blancas/morph "0.3.0" :exclusions [org.clojure/clojure]]
                                  [com.taoensso/timbre "4.0.2" :exclusions [org.clojure/clojure]]
                                  [org.clojure/math.numeric-tower "0.0.4"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0-alpha3"]]}
             }

  :test-paths ["test" "examples"]

  :plugins [[lein-midje "3.2-RC4"]
            [codox "0.8.11"]]

  :codox {:src-dir-uri "https://github.com/marick/structural-typing/blob/master/"
          :src-linenum-anchor-prefix "L"
          :output-dir "/var/tmp/structural-typing-doc"
          :defaults {:doc/format :markdown}}

  :aliases {"compatibility" ["with-profile" "+1.7:+1.8" "midje" ":config" ".compatibility-test-config"]
            "travis" ["with-profile" "+1.7:+1.8" "midje"]}

  ;; For Clojure snapshots
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :deploy-repositories [["releases" :clojars]]
)
