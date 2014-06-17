(defproject metosin/ring-swagger "0.9.1"
  :description "Swagger Spec for Ring Apps"
  :url "https://github.com/metosin/ring-swagger"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire "5.3.1"]
                 [slingshot "0.10.3"]
                 [metosin/ring-http-response "0.4.0"]
                 [prismatic/schema "0.2.3"]
                 [prismatic/plumbing "0.3.1"]
                 [clj-time "0.7.0"]
                 [camel-snake-kebab "0.1.5"]
                 [ring/ring-core "1.3.0"]]
  :plugins [[lein-ring "0.8.10"]]
  :profiles {:dev {:plugins [[lein-clojars "0.9.1"]
                             [lein-midje "3.1.3"]]
                   :dependencies [[midje "1.6.3"]
                                  [ring-mock "0.1.5"]
                                  [metosin/ring-swagger-ui "2.0.16-2"]
                                  [javax.servlet/servlet-api "2.5"]]}})
