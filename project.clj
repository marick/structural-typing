(defproject marick/structural-typing "0.9.0"
  :description "Structural typing for Clojure. Generally useful when you want to define predicates to apply in bulk to parts of structures. The top-level namespaces contain what you need to use the library. The .api namespaces help you tweak its behavior."
  :url "https://github.com/marick/structural-typing"
  :pedantic? :warn
  :license {:name "The Unlicense"
            :url "http://unlicense.org/"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [marick/suchwow "3.3.0"]
                 [com.rpl/specter "0.6.2" :exclusions [org.clojure/clojure]]]

  :profiles {:dev {:dependencies [[midje "1.7.0" :exclusions [org.clojure/clojure]]
                                  [org.blancas/morph "0.3.0" :exclu<sions [org.clojure/clojure]]
                                  [com.taoensso/timbre "4.0.2" :exclusions [org.clojure/clojure]]
                                  [org.clojure/math.numeric-tower "0.0.4"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             }

  :test-paths ["test" "examples"]

  :plugins [[lein-midje "3.1.3"]
            [codox "0.8.11"]]

  :codox {:src-dir-uri "https://github.com/marick/structural-typing/blob/master/"
          :src-linenum-anchor-prefix "L"
          :output-dir "/var/tmp/structural-typing-doc"
          :defaults {:doc/format :markdown}}

  :aliases {"compatibility" ["with-profile" "+1.7" "midje" ":config" ".compatibility-test-config"]
            "travis" ["with-profile" "+1.7" "midje"]}

  ;; For Clojure snapshots
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :deploy-repositories [["releases" :clojars]]
)
