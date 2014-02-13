(defproject metosin/ring-swagger "0.3.0"
  :description "Swagger Spec for Ring Apps"
  :url "https://github.com/metosin/ring-swagger"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [cheshire "5.3.1"]
                 [camel-snake-kebab "0.1.2"]
                 [prismatic/schema "0.2.0"]
                 [ring/ring-core "1.2.1"]]
  :plugins [[lein-ring "0.8.7"]]
  :ring {:handler compojure.api.example.handler/app}
  :profiles {:dev {:plugins [[lein-clojars "0.9.1"]
                             [lein-midje "3.1.1"]]
                   :dependencies [[midje "1.6.0"]]}})
