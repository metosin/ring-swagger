(ns ring.swagger.ui-test
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [ring.swagger.test-utils :refer :all]
            [ring.swagger.ui :refer :all]))

(tabular
  (fact get-path
    (get-path ?root ?uri) => ?path)

  ?root           ?uri                      ?path
  ""              "/"                       ""
  "/"             "/"                       ""
  ""              "/index.html"             "index.html"
  "/"             "/index.html"             "index.html"
  "/ui-docs"      "/index.html"             nil
  "/ui-docs"      "/ui-docs/index.html"     "index.html"
  )

;; Utils for testing swagger-ui with real requests

(defn GET [app uri & kwargs]
  (app (merge (mock/request :get uri) (apply hash-map kwargs))))

(defn status? [{:keys [status]} expected]
  (= status expected))

(defn redirect? [uri]
  (fn [{{location "Location"} :headers :as res}]
    (and (status? res 302) (= location uri))))

(defn html? [{{content-type "Content-Type"} :headers :as res}]
  (and (status? res 200) (= content-type "text/html")))

(defn javascript? [{{content-type "Content-Type"} :headers :as res}]
  (and (status? res 200) (= content-type "application/javascript")))

(facts "Swagger-UI"
  (facts "(swagger-ui)"
    (let [handler (swagger-ui)
          GET (partial GET handler)]
      ;; Uri will always start with "/"
      (GET "/") => (redirect? "/index.html")
      (GET "/index.html") => html?))
  (facts "(swagger-ui \"/ui-docs\")"
    (let [handler (swagger-ui "/ui-docs")
          GET (partial GET handler)]
      (GET "/") => nil
      (GET "/index.html") => nil
      ;; Request to directory, uri doesn't necessarily end with "/"
      (GET "/ui-docs/") => (redirect? "/ui-docs/index.html")
      (GET "/ui-docs") => (redirect? "/ui-docs/index.html")
      (GET "/ui-docs/index.html") => html?
      (GET "/ui-docs/conf.js") => javascript?))
  ;; Some possible envinronments
  (facts "Compojure"
    (fact "(context \"/compojure\" [] (swagger-ui))"
      (GET (swagger-ui) "/compojure" :context "/compojure") => (redirect? "/compojure/index.html"))
    (fact "(context \"/compojure\" [] (swagger-ui \"/docs\"))"
      (GET (swagger-ui "/docs") "/compojure/docs" :context "/compojure") => (redirect? "/compojure/docs/index.html")))
  (facts "Servlet context"
    ;; Servlet context:
    ;; - uri will contain full path
    ;; - servlet context countains ServletContext object
    ;; - context contains context path (under compojure this will also contain compojure context)
    (let [fake-context  (fake-servlet-context "/servlet")]
      (fact "(swagger-ui)"
        (GET (swagger-ui) "/servlet" :context "/servlet" :servlet-context fake-context) => (redirect? "/servlet/index.html"))
      (fact "(swagger-ui \"/docs\")"
        (GET (swagger-ui "/docs") "/servlet/docs" :context "/servlet" :servlet-context fake-context) => (redirect? "/servlet/docs/index.html"))

      (facts "Compojure"
        (fact "(context \"/compojure\" [] (swagger-ui))"
          (GET (swagger-ui) "/servlet/compojure" :context "/servlet/compojure" :servlet-context fake-context) => (redirect? "/servlet/compojure/index.html"))
        (fact "(context \"/compojure\" [] (swagger-ui \"/docs\"))"
          (GET (swagger-ui "/docs") "/servlet/compojure/docs" :context "/servlet/compojure" :servlet-context fake-context) => (redirect? "/servlet/compojure/docs/index.html"))))))

(facts "wrap-swagger-ui"
  (let [handler (wrap-swagger-ui (fn [_] ..response..))
        GET (partial GET handler)]
    (fact "servers swagger-ui resources"
      (GET "/") => (redirect? "/index.html")
      (GET "/index.html") => html?)
    (fact "forwards non-swagger-ui resources"
      (GET "/NON-SWAGGER-FILE") => ..response..)))

(facts "conf.js"

  (fact "with default parameters"
    (conf-js nil {})
    => "window.API_CONF = {\"url\":\"/swagger.json\"};")

  (fact "with swagger-docs & oauth2 set"
    (conf-js nil {:swagger-docs "/lost"
                  :oauth2       {:client-id "1" :app-name "2" :realm "3"}})
    => "window.API_CONF = {\"oauth2\":{\"realm\":\"3\",\"app-name\":\"2\",\"client-id\":\"1\"},\"url\":\"/lost\"};")

  (fact "does not fail with crappy input"
    (conf-js nil {:kikka "kukka"
                  :oauth2 {:abba "jabba"}}) => string?))
