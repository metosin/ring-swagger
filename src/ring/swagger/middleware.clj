(ns ring.swagger.middleware
  (:require [schema.utils :as su]
            [schema.core :as s]
            [ring.swagger.schema :as rss]
            [plumbing.core :as p]
            [clojure.walk :as walk]
            [ring.util.http-response :as http-response]
            [ring.swagger.common :as common])
  (:import (schema.utils ValidationError)))

;;
;; middleware-based swagger parameters
;;

(defn set-swagger-data
  "Sets extra top-level swagger-data into a request.
  By default, deep-merges gives data in. Data
  can be read with get-swagger-data."
  ([request data]
   (set-swagger-data request common/deep-merge data))
  ([request f & data]
   (update-in request [::data] (partial apply f) data)))

(defn get-swagger-data
  "Reads top-level swagger-data from request, pushed in by
  set-swagger-data."
  [request]
  (::data request))

(defn wrap-swagger-data
  "Middleware that adds top level swagger-data into request."
  [handler data]
  (fn
    ([request]
     (handler (set-swagger-data request data)))
    ([request respond raise]
     (handler (set-swagger-data request data) respond raise))))

;;
;; common utilities
;;

(defn comp-mw [mw & base-params]
  (fn [handler & params]
    (apply mw (concat [handler] base-params params))))

(defn stringify-error [error]
  (walk/postwalk
    (fn [x]
      (if-not (map? x)
        x
        (p/for-map [[k v] x]
          k (cond
              (instance? ValidationError v) (str (su/validation-error-explain v))
              (symbol? v) (str v)
              :else v))))
    error))

(defn default-error-handler [{:keys [error]}]
  (http-response/bad-request {:errors (stringify-error error)}))

(defn- handle-exception [catch-core-errors? error-handler e respond raise]
  (let [{:keys [type] :as data} (ex-data e)]
    (if (or (and catch-core-errors? (= type ::s/error))
            (= type ::rss/validation))
      (respond (error-handler data))
      (raise e))))

(defn wrap-validation-errors
  "Middleware that catches thrown ring-swagger validation errors turning them
   into valid error respones. Accepts the following options:

   :error-handler - a function of schema.utils.ErrorContainer -> response
   :catch-core-errors? - consume also :schema.core/errors (defaults to false)"
  [handler & [{:keys [error-handler catch-core-errors?]}]]
  (let [error-handler (or error-handler default-error-handler)
        handle-exception (partial handle-exception catch-core-errors? error-handler)
        throw #(throw %)]
    (fn
      ([request]
       (try
         (handler request)
         (catch Exception e
           (handle-exception e identity throw))))
      ([request respond raise]
       (try
         (handler request respond #(handle-exception % respond raise))
         (catch Exception e
           (handle-exception e respond throw)))))))
