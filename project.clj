(defproject metosin/ring-swagger "0.19.1-SNAPSHOT"
  :description "Swagger Spec for Ring Apps"
  :url "https://github.com/metosin/ring-swagger"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.4.0"]
                 [slingshot "0.12.2"]
                 [metosin/ring-http-response "0.6.0"]
                 [prismatic/schema "0.4.0"]
                 [prismatic/plumbing "0.4.0"]
                 [clj-time "0.9.0"]
                 [org.tobereplaced/lettercase "1.0.0"]
                 [ring/ring-core "1.3.2"]
                 [potemkin "0.3.12"]
                 [org.flatland/ordered "1.5.2"]
                 [instar "1.0.10" :exclusions [org.clojure/clojure]]]
  :profiles {:dev {:plugins [[lein-clojars "0.9.1"]
                             [lein-ring "0.9.2"]
                             [lein-midje "3.1.3"]]
                   :dependencies [[midje "1.7.0-SNAPSHOT"]
                                  [ring-mock "0.1.5"]
                                  [com.github.fge/json-schema-validator "2.2.6"]
                                  [metosin/ring-swagger-ui "2.0.24"]
                                  [javax.servlet/servlet-api "2.5"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha5"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.7"]
            "test-ancient" ["midje"]})
