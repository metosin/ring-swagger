(ns ring.swagger.ui
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

(defn conf-js [req opts]
  (let [swagger-docs (swagger/join-paths (swagger/context req) (:swagger-docs opts "/swagger.json"))
        conf (-> opts
                 (dissoc :swagger-docs)
                 (assoc :url swagger-docs))]
    (str "window.API_CONF = " (json/generate-string conf {:key-fn json-key}) ";")))

(defn swagger-ui
  "This function creates a ring handler which can be used to serve swagger-ui.
   If the first parameter is a String, it used as context for swagger-ui,
   default is to serve swagger-ui at \"/\".

   Other options can be given using keyword-value pairs.

   - **:root** the root prefix to get resources from. Default 'swagger-ui'
   - **:swagger-docs** the endpoint to get swagger data from. Default '/swagger.json'
   - **:oauth2** map with oauth2 params, namely `:client-id`, `:realm` and `:app-name`
   - Other options are passed as is to SwaggerUi constructor. Map keys are
   renamed to camelCase."
  {:arglists '([context & {:as opts}]
               [& {:as opts}])}
  [& params]
  (let [[path kvs] (if (string? (first params))
                     [(first params) (rest params)]
                     ["/" params])
        options (apply hash-map kvs)
        root (:root options "swagger-ui")]
    (-> (fn [{:keys [uri] :as req}]
          (let [;; Prefix path with servlet-context and compojure context
                path (swagger/join-paths (:context req) path)]
            ;; Check if requested uri is under swagger-ui path and what file is requested
            (when-let [req-path (get-path path uri)]
              (condp = req-path
                "" (http-response/found (swagger/join-paths uri "index.html"))
                "conf.js" (http-response/content-type (http-response/ok (conf-js req options)) "application/javascript")
                (http-response/resource-response (str root "/" req-path))))))
        (content-type/wrap-content-type options)
        (not-modified/wrap-not-modified)
        (head/wrap-head))))

(defn wrap-swagger-ui
  "Middleware to serve the swagger-ui."
  [handler & params]
  (let [ui (apply swagger-ui params)]
    (fn [request]
      (or (ui request) (handler request)))))
