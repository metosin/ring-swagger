(defproject metosin/ring-swagger "0.26.2"
  :description "Swagger Spec for Ring Apps"
  :url "https://github.com/metosin/ring-swagger"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[cheshire "5.10.1"]
                 [metosin/ring-http-response "0.9.3"]
                 [ring/ring-core "1.9.4"]
                 [metosin/schema-tools "0.12.3"]
                 [prismatic/schema "1.2.0"]
                 [prismatic/plumbing "0.6.0"]
                 [metosin/scjsv "0.6.2"]
                 [clj-time "0.15.2"]
                 [org.tobereplaced/lettercase "1.0.0"]
                 [potemkin "0.4.5"]
                 [frankiesardo/linked "1.3.0"]]
  :profiles {:dev {:plugins [[lein-clojars "0.9.1"]
                             [lein-ring "0.12.5"]
                             [lein-midje "3.2.1"]
                             [funcool/codeina "0.5.0"]]
                   :dependencies [[org.clojure/clojure "1.10.3"]
                                  [midje "1.10.5"
                                   :exclusions [org.clojure/clojure
                                                commons-codec]]
                                  [ring-mock "0.1.5"]
                                  [metosin/ring-swagger-ui "4.0.0"]
                                  [javax.servlet/javax.servlet-api "4.0.1"]]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}}
  :codeina {:sources ["src"]
            :target "gh-pages/doc"
            :src-uri "http://github.com/metosin/ring-swagger/blob/master/"
            :src-uri-prefix "#L"}
  :deploy-repositories [["releases" :clojars]]
  :aliases {"all" ["with-profile" "dev:dev,1.9"]
            "test-ancient" ["midje"]})
