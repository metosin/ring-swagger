(ns ring.swagger.swagger-ui-test
  (:require [midje.sweet :refer :all]
            [ring.mock.request :as mock]
            [ring.swagger.test-utils :as test-utils]
            [ring.swagger.swagger-ui :refer :all]
            [cheshire.core :as json]))

(tabular
  (fact get-path
    (get-path ?root ?uri) => ?path)

  ?root ?uri ?path
  "" "/" ""
  "/" "/" ""
  "" "/index.html" "index.html"
  "/" "/index.html" "index.html"
  "/ui-docs" "/index.html" nil
  "/ui-docs" "/ui-docs/index.html" "index.html")

(defn http-get-sync
  ([app uri]
   (http-get-sync app uri {}))
  ([app uri options]
   (app (merge (mock/request :get uri) options))))

(defn http-get-async
  ([app uri]
   (http-get-async app uri {}))
  ([app uri options]
   (let [respond (promise)]
     (app (merge (mock/request :get uri) options) respond (promise))
     @respond)))

(defn status? [{:keys [status]} expected]
  (= status expected))

(defn redirect? [uri]
  (fn [{{location "Location"} :headers :as res}]
    (and (status? res 302) (= location uri))))

(defn html? [{{content-type "Content-Type"} :headers :as res}]
  (and (status? res 200) (= content-type "text/html")))

(defn javascript? [{{content-type "Content-Type"} :headers :as res}]
  (and (status? res 200) (= content-type "application/javascript")))

(facts "swagger-ui"

  (doseq [[mode http-get] [["sync" http-get-sync]
                           ["async" http-get-async]]]

    (facts {:midje/description mode}

      (facts "without parameters"
        (let [handler (swagger-ui)]
          ;; Uri will always start with "/"
          (http-get handler "/") => (redirect? "/index.html")
          (http-get handler "/index.html") => html?))

      (facts "with uri"
        (let [handler (swagger-ui {:path "/ui-docs"})]

          (http-get handler "/") => nil
          (http-get handler "/index.html") => nil

          ;; Request to directory, uri doesn't necessarily end with "/"
          (http-get handler "/ui-docs/") => (redirect? "/ui-docs/index.html")
          (http-get handler "/ui-docs") => (redirect? "/ui-docs/index.html")
          (http-get handler "/ui-docs/index.html") => html?
          (http-get handler "/ui-docs/conf.js") => javascript?))

      ;; Some possible envinronments
      (facts "Under a compojure context"

        (fact "with context"
          (http-get
            (swagger-ui)
            "/compojure"
            {:context "/compojure"}) => (redirect? "/compojure/index.html"))

        (fact "with context and an uri"
          (http-get
            (swagger-ui {:path "/docs"})
            "/compojure/docs"
            {:context "/compojure"}) => (redirect? "/compojure/docs/index.html")))

      (facts "Servlet context"
        ;; Servlet context:
        ;; - uri will contain full path
        ;; - servlet context countains ServletContext object
        ;; - context contains context path (under compojure this will also contain compojure context)
        (let [fake-context (test-utils/fake-servlet-context "/servlet")]
          (http-get (swagger-ui)
                    "/servlet"
                    {:context "/servlet"
                     :servlet-context fake-context}) => (redirect? "/servlet/index.html")

          (fact "with uri"
            (http-get (swagger-ui {:path "/docs"})
                      "/servlet/docs"
                      {:context "/servlet"
                       :servlet-context fake-context}) => (redirect? "/servlet/docs/index.html"))

          (facts "Compojure with servlet context"
            (http-get (swagger-ui)
                      "/servlet/compojure"
                      {:context "/servlet/compojure"
                       :servlet-context fake-context}) => (redirect? "/servlet/compojure/index.html")

            (fact "and uri"
              (http-get (swagger-ui {:path "/docs"})
                        "/servlet/compojure/docs"
                        {:context "/servlet/compojure"
                         :servlet-context fake-context}) => (redirect? "/servlet/compojure/docs/index.html"))))))))

(facts "wrap-swagger-ui"
  (let [handler (wrap-swagger-ui (constantly ..response..))
        http-get (partial http-get-sync handler)]

    (fact "servers swagger-ui resources"
      (http-get "/") => (redirect? "/index.html")
      (http-get "/index.html") => html?)

    (fact "forwards non-swagger-ui resources"
      (http-get "/NON-SWAGGER-FILE") => ..response..)))

(defn- strip-js [s]
  (second (re-find #"window\.API_CONF = (.*);" s)))

(defn- read-js [s]
  (-> s strip-js (json/parse-string)))

(facts "conf.js"

  (fact "with default parameters"
    (conf-js nil {})
    => "window.API_CONF = {\"url\":\"/swagger.json\"};")

  (fact "with default parameters"
    (read-js (conf-js nil {}))
    => {"url" "/swagger.json"})

  (fact "with swagger-docs & oauth2 set"
    (read-js (conf-js nil {:swagger-docs "/lost"
                           :oauth2 {:client-id "1"
                                    :app-name "2"
                                    :realm "3"}}))
    => {"url" "/lost"
        "oauth2" {"clientId" "1"
                  "appName" "2"
                  "realm" "3"}})

  (fact "with parameter passthrough"
    (read-js (conf-js nil {:validatorUrl "foo"}))
    => {"url" "/swagger.json"
        "validatorUrl" "foo"})

  (fact "with parameter passthrough and key renaming"
    (read-js (conf-js nil {:validator-url "foo"}))
    => {"url" "/swagger.json"
        "validatorUrl" "foo"})

  (fact "does not fail with crappy input"
    (conf-js nil {:kikka "kukka"
                  :oauth2 {:abba "jabba"}}) => string?))
