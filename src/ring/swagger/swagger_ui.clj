(ns ring.swagger.swagger-ui
  (:require [cheshire.core :as json]
            [ring.util.http-response :as http-response]
            [ring.middleware.content-type :as content-type]
            [ring.middleware.not-modified :as not-modified]
            [ring.middleware.head :as head]
            [ring.swagger.core :as swagger]
            [org.tobereplaced.lettercase :as lc]))

(defn get-path [root uri]
  (second (re-find (re-pattern (str "^" root "[/]?(.*)")) uri)))

(defn- json-key [k]
  (lc/mixed (name k)))

(defn config-json [req opts]
  (let [swagger-docs (swagger/join-paths (swagger/context req) (:swagger-docs opts "/swagger.json"))
        conf (-> opts
                 (dissoc :swagger-docs :path)
                 (assoc :url swagger-docs))]
    (json/generate-string conf {:key-fn json-key})))


(defn- serve [{:keys [path root] :or {path "/", root "swagger-ui"} :as options}]
  (let [f (fn [{request-uri :uri :as req}]
            (let [;; Prefix path with servlet-context and compojure context
                  uri (swagger/join-paths (:context req) path)]
              ;; Check if requested uri is under swagger-ui path and what file is requested
              (when-let [req-path (get-path uri request-uri)]
                (condp = req-path
                  "" (http-response/found (swagger/join-paths request-uri "index.html"))
                  "config.json" (http-response/content-type (http-response/ok (config-json req options)) "application/json")
                  (http-response/resource-response (str root "/" req-path))))))]
    (fn
      ([request]
       (f request))
      ([request respond _]
        (respond (f request))))))

(defn swagger-ui
  "Returns a (async-)ring handler which can be used to serve swagger-ui.
   Takes the following options:

   - **:path**         the root path for the swagger-ui, Defaults to \"/\"
   - **:root**         the root prefix to get resources from. Default 'swagger-ui'
   - **:swagger-docs** the endpoint to get swagger data from. Default '/swagger.json'
   - **:oauth2**       map with oauth2 params, namely `:client-id`, `:realm` and `:app-name`
   - Other options are passed as is to SwaggerUi constructor. Map keys are
   renamed to camelCase."
  ([]
   (swagger-ui {}))
  ([options]
   {:pre [(map? options)]}
   (-> (serve options)
       (content-type/wrap-content-type options)
       (not-modified/wrap-not-modified)
       (head/wrap-head))))

(defn wrap-swagger-ui
  "Middleware to serve the swagger-ui."
  ([handler]
   (wrap-swagger-ui handler {}))
  ([handler options]
   (let [ui (swagger-ui options)]
     (fn
       ([request]
        ((some-fn ui handler) request))
       ([request respond raise]
        (ui request
            #(if % (respond %) (handler request respond raise))
            raise))))))
