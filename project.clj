(defproject metosin/ring-swagger "0.14.1"
  :description "Swagger Spec for Ring Apps"
  :url "https://github.com/metosin/ring-swagger"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.3.1"]
                 [slingshot "0.12.1"]
                 [metosin/ring-http-response "0.5.1"]
                 [prismatic/schema "0.3.2"]
                 [prismatic/plumbing "0.3.5"]
                 [clj-time "0.8.0"]
                 [org.tobereplaced/lettercase "1.0.0"]
                 [ring/ring-core "1.3.1"]
                 [potemkin "0.3.11"]
                 [org.flatland/ordered "1.5.2"]]
  :plugins [[lein-ring "0.8.13"]]
  :profiles {:dev {:plugins [[lein-clojars "0.9.1"]
                             [lein-midje "3.1.3"]]
                   :dependencies [[midje "1.6.3"]
                                  [ring-mock "0.1.5"]
                                  [metosin/ring-swagger-ui "2.0.17"]
                                  [javax.servlet/servlet-api "2.5"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha2"]]}}
  :aliases {"all" ["with-profile" "dev:dev,1.7"]
            "test-ancient" ["midje"]})
