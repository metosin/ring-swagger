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

(facts "Swagger-UI"
  (facts "(swagger-ui)"
    (let [handler (swagger-ui)
          GET (partial GET handler)]
      ;; Uri will always start with "/"
      (GET "/") => (redirect? "/index.html")
      (GET "/index.html") => html?
      ))
  (facts "(swagger-ui \"/ui-docs\")"
    (let [handler (swagger-ui "/ui-docs")
          GET (partial GET handler)]
      (GET "/") => nil
      (GET "/index.html") => nil
      ;; Request to directory, uri doesn't necessarily end with "/"
      (GET "/ui-docs/") => (redirect? "/ui-docs/index.html")
      (GET "/ui-docs") => (redirect? "/ui-docs/index.html")
      (GET "/ui-docs/index.html") => html?
      ))
  ;; Some possible envinronments
  (facts "Compojure"
    (fact "(context \"/compojure\" [] (swagger-ui))"
      (GET (swagger-ui) "/compojure" :context "/compojure") => (redirect? "/compojure/index.html"))
    (fact "(context \"/compojure\" [] (swagger-ui \"/docs\"))"
      (GET (swagger-ui "/docs") "/compojure/docs" :context "/compojure") => (redirect? "/compojure/docs/index.html"))
    )
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
          (GET (swagger-ui "/docs") "/servlet/compojure/docs" :context "/servlet/compojure" :servlet-context fake-context) => (redirect? "/servlet/compojure/docs/index.html"))
        ))))
