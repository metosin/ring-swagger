(defproject metosin/ring-swagger "0.26.2"
  :description "Swagger Spec for Ring Apps"
  :url "https://github.com/metosin/ring-swagger"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[cheshire "5.8.1"]
                 [metosin/ring-http-response "0.9.1"]
                 [ring/ring-core "1.7.1"]
                 [metosin/schema-tools "0.11.0"]
                 [prismatic/schema "1.1.10"]
                 [prismatic/plumbing "0.5.5"]
                 [metosin/scjsv "0.5.0"]
                 [clj-time "0.15.1"]
                 [org.tobereplaced/lettercase "1.0.0"]
                 [potemkin "0.4.5"]
                 [frankiesardo/linked "1.3.0"]]
  :profiles {:dev {:plugins [[lein-clojars "0.9.1"]
                             [lein-ring "0.12.5"]
                             [lein-midje "3.2.1"]
                             [funcool/codeina "0.5.0"]]
                   :dependencies [[org.clojure/clojure "1.8.0"]
                                  [midje "1.9.6" :exclusions [org.clojure/clojure
                                                              commons-codec]]
                                  [ring-mock "0.1.5"]
                                  [metosin/ring-swagger-ui "3.20.1"]
                                  [javax.servlet/javax.servlet-api "4.0.1"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :1.10 {:dependencies [[org.clojure/clojure "1.10.1"]]}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.1"]]}
             :1.12 {:dependencies [[org.clojure/clojure "1.12.0-alpha5"]]}}
  :codeina {:sources ["src"]
            :target "gh-pages/doc"
            :src-uri "http://github.com/metosin/ring-swagger/blob/master/"
            :src-uri-prefix "#L"}
  :deploy-repositories [["releases" :clojars]]
  :aliases {"all" ["with-profile" "dev:dev,1.7:dev,1.9:dev,1.10:dev,1.11:dev,1.12"]
            "test-ancient" ["midje"]})
