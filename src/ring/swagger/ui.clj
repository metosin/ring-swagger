(ns ring.swagger.ui
  (:require [ring.util.response :as response]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.head :refer [wrap-head]]
            [ring.swagger.core :as swagger]))

(defn get-path [root uri]
  (second (re-find (re-pattern (str "^" root "[/]?(.*)")) uri)))

(defn conf-js [req {:keys [swagger-docs] :or {swagger-docs "/api/api-docs"}}]
  (let [swagger-docs (swagger/join-paths (swagger/context req) swagger-docs)]
    (str "window.API_CONF = {url: \"" swagger-docs "\"};")))

(defn swagger-ui
  "This function creates a ring handler which can be used to serve swagger-ui.
   If the first parameter is a String, it used as context for swagger-ui,
   default is to serve swagger-ui at \"/\".

   Other options can be given using keyword-value pairs.
   :root - the root prefix to get resources from. Default 'swagger-ui'
   :swagger-docs - the endpoint to get swagger data from. Default '/api/docs'"
  [& params]
  (let [[path kvs] (if (string? (first params))
                     [(first params) (rest params)]
                     ["/" params])
        options (apply hash-map kvs)
        root (:root options "swagger-ui")]
    (-> (fn [{:keys [http-method uri] :as req}]
          (let [;; Prefix path with servlet-context and compojure context
                path (swagger/join-paths (:context req) path)]
            ;; Check if requested uri is under swagger-ui path and what file is requested
            (when-let [req-path (get-path path uri)]
              (condp = req-path
                "" (response/redirect (swagger/join-paths uri "index.html"))
                "conf.js" (response/response (conf-js req options))
                (response/resource-response (str root "/" req-path))))))
        (wrap-content-type options)
        (wrap-not-modified)
        (wrap-head))))
