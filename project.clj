(defproject metosin/ring-swagger "0.22.15-SNAPSHOT"
  :description "Swagger Spec for Ring Apps"
  :url "https://github.com/metosin/ring-swagger"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[cheshire "5.7.0"]
                 [slingshot "0.12.2"]
                 [metosin/ring-http-response "0.8.1"]
                 [metosin/schema-tools "0.9.0"]
                 [prismatic/schema "1.1.3"]
                 [prismatic/plumbing "0.5.3"]
                 [metosin/scjsv "0.4.0"]
                 [clj-time "0.13.0"]
                 [org.tobereplaced/lettercase "1.0.0"]
                 [potemkin "0.4.3"]
                 [frankiesardo/linked "1.2.9"]]
  :profiles {:dev {:plugins [[lein-clojars "0.9.1"]
                             [lein-ring "0.10.0"]
                             [lein-midje "3.2.1"]
                             [funcool/codeina "0.5.0"]]
                   :dependencies [[org.clojure/clojure "1.8.0"]
                                  [midje "1.8.3"]
                                  [ring-mock "0.1.5"]
                                  [metosin/ring-swagger-ui "2.2.8"]
                                  [javax.servlet/servlet-api "2.5"]
                                  ; Required when using with Java 1.6
                                  [org.codehaus.jsr166-mirror/jsr166y "1.7.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.9 {:dependencies [[org.clojure/clojure "1.9.0-alpha14"]]}}
  :codeina {:sources ["src"]
            :target "gh-pages/doc"
            :src-uri "http://github.com/metosin/ring-swagger/blob/master/"
            :src-uri-prefix "#L"}
  :deploy-repositories [["releases" :clojars]]
  :aliases {"all" ["with-profile" "dev:dev,1.7"]
            "test-ancient" ["midje"]})
