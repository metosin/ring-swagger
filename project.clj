(defproject metosin/ring-swagger "0.7.4"
  :description "Swagger Spec for Ring Apps"
  :url "https://github.com/metosin/ring-swagger"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.3.1"]
                 [slingshot "0.10.3"]
                 [metosin/ring-http-response "0.3.0"]
                 [prismatic/schema "0.2.1"]
                 [clj-time "0.6.0"]
                 [camel-snake-kebab "0.1.4"]
                 [ring/ring-core "1.2.1"]]
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler compojure.api.example.handler/app}
  :profiles {:dev {:plugins [[lein-clojars "0.9.1"]
                             [lein-midje "3.1.3"]]
                   :dependencies [[midje "1.6.2"]]}})
