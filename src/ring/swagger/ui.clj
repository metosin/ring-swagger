(ns ring.swagger.ui
  (:require [ring.util.response :as response]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.head :refer [wrap-head]]))

(defn get-path [root uri]
  (nth (re-find (re-pattern (str "^" root "[/]?(.*)")) uri) 1))

(defn conf-js [{:keys [swagger-docs] :or {swagger-docs "/api/api-docs"}}]
  (str "window.API_CONF = {url: \"" swagger-docs "\"};"))

(defn swagger-ui [& params]
  "Route creates a ring handler which will serve swagger-ui.
   If the first parameter is a String, it used as context for swagger-ui,
   default is to serve swagger-ui at \"/\".

   Other options can be given using keyword-value pairs.
   :root - the root prefix to get resources from. Default 'swagger-ui'
   :swagger-docs - the endpoint to get swagger data from. Default '/api/docs'"
  (let [[path kvs] (if (string? (first params))
                     [(first params) (rest params)]
                     ["/" params])
        options (apply hash-map kvs)]
    (-> (fn [{:keys [http-method uri] :as req}]
          (when-let [req-path (get-path path uri)]
            (let [root (:root options "swagger-ui")]
              (condp = req-path
                "" (response/redirect (str path (if (.endsWith path "/") "" "/") "index.html"))
                "conf.js" (response/response (conf-js options))
                (response/resource-response (str root "/" req-path))))))
        (wrap-file-info (:mime-types options))
        (wrap-content-type options)
        (wrap-head))))
