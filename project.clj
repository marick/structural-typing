(defproject marick/structural-typing "0.1.0"
  :description "Extended structural typing for Clojure"
  :url "https://github.com/marick/structural-typing"
  :pedantic? :warn
  :license {:name "The Unlicense"
            :url "http://unlicense.org/"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [marick/suchwow "2.2.0" :exclusions [org.clojure/clojure]]
                 [bouncer "0.3.2" :exclusions [org.clojure/clojure]]]

  :profiles {:dev {:dependencies [[midje "1.7.0-beta1" :exclusions [org.clojure/clojure]]
                                  [org.blancas/morph "0.3.0" :exclusions [org.clojure/clojure]]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5.0 {:dependencies [[org.clojure/clojure "1.5.0"]]}
             :1.5.1 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-beta3"]]}
             }

  :plugins [[lein-midje "3.1.3"]
            [codox "0.8.11"]]

  :codox {:src-dir-uri "https://github.com/marick/structural-typing/blob/master/"
          :src-linenum-anchor-prefix "L"
          :output-dir "/var/tmp/structural-typing-doc"
          :defaults {:doc/format :markdown}}

  :aliases {"compatibility" ["with-profile" "+1.4:+1.5.0:+1.5.1:+1.6:+1.7" "midje" ":config" ".compatibility-test-config"]
            "travis" ["with-profile" "+1.4:+1.5.0:+1.5.1:+1.6:+1.7" "midje"]}

  ;; For Clojure snapshots
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :deploy-repositories [["releases" :clojars]]
)
