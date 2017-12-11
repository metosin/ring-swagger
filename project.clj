(defproject metosin/ring-swagger "0.24.3"
  :description "Swagger Spec for Ring Apps"
  :url "https://github.com/metosin/ring-swagger"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[cheshire "5.8.0"]
                 [metosin/ring-http-response "0.9.0"]
                 [ring/ring-core "1.6.3"]
                 [metosin/schema-tools "0.9.1"]
                 [prismatic/schema "1.1.7"]
                 [prismatic/plumbing "0.5.4"]
                 [metosin/scjsv "0.4.0"]
                 [clj-time "0.14.0"]
                 [org.tobereplaced/lettercase "1.0.0"]
                 [potemkin "0.4.4"]
                 [frankiesardo/linked "1.2.9"]]
  :profiles {:dev {:plugins [[lein-clojars "0.9.1"]
                             [lein-ring "0.12.1"]
                             [lein-midje "3.2.1"]
                             [funcool/codeina "0.5.0"]]
                   :dependencies [[org.clojure/clojure "1.8.0"]
                                  [midje "1.9.0-alpha6"]
                                  [ring-mock "0.1.5"]
                                  [metosin/ring-swagger-ui "3.0.17"]
                                  [javax.servlet/javax.servlet-api "3.1.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0"]]}}
  :codeina {:sources ["src"]
            :target "gh-pages/doc"
            :src-uri "http://github.com/metosin/ring-swagger/blob/master/"
            :src-uri-prefix "#L"}
  :deploy-repositories [["releases" :clojars]]
  :aliases {"all" ["with-profile" "dev:dev,1.7:dev,1.9"]
            "test-ancient" ["midje"]})
