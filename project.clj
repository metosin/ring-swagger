(defproject metosin/ring-swagger "0.20.1"
  :description "Swagger Spec for Ring Apps"
  :url "https://github.com/metosin/ring-swagger"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.4.0"]
                 [slingshot "0.12.2"]
                 [metosin/ring-http-response "0.6.1"]
                 [metosin/schema-tools "0.4.0"]
                 [prismatic/schema "0.4.1"]
                 [prismatic/plumbing "0.4.2"]
                 [clj-time "0.9.0"]
                 [org.tobereplaced/lettercase "1.0.0"]
                 [potemkin "0.3.13"]
                 [org.flatland/ordered "1.5.2"]
                 [instar "1.0.10" :exclusions [org.clojure/clojure com.keminglabs/cljx org.clojure/clojurescript]]]
  :profiles {:dev {:plugins [[lein-clojars "0.9.1"]
                             [lein-ring "0.9.3"]
                             [lein-midje "3.1.3"]]
                   :dependencies [[midje "1.7.0-SNAPSHOT"]
                                  [ring-mock "0.1.5"]
                                  [metosin/scjsv "0.2.0"]
                                  [metosin/ring-swagger-ui "2.0.24"]
                                  [javax.servlet/servlet-api "2.5"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha4"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.7"]
            "test-ancient" ["midje"]})
