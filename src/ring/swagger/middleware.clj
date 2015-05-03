(ns ring.swagger.middleware
  (:require [slingshot.slingshot :refer [try+ throw+]]
            [schema.utils :as su]
            [ring.swagger.common :refer [deep-merge]]
            [plumbing.core :refer [for-map]]
            [clojure.walk :refer :all]
            [ring.util.http-response :refer [bad-request]])
  (:import (schema.utils ValidationError)))

;;
;; middleware-based swagger parameters
;;

(defn set-swagger-data
  "Sets extra top-level swagger-data into a request.
  By default, deep-merges gives data in. Data
  can be read with get-swagger-data."
  ([request data]
   (set-swagger-data request deep-merge data))
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
  (fn [request]
    (handler (set-swagger-data request data))))

;;
;; common utilities
;;

(defn comp-mw [mw & base-params]
  (fn [handler & params]
    (apply mw (concat [handler] base-params params))))

(defn stringify-error [error]
  (postwalk
    (fn [x]
      (if-not (map? x)
        x
        (for-map [[k v] x]
          k (cond
              (instance? ValidationError v) (str (su/validation-error-explain v))
              (symbol? v) (str v)
              :else v))))
    error))

(defn default-error-handler [{:keys [error]}]
  (bad-request {:errors (stringify-error error)}))

(defn wrap-validation-errors
  "Middleware that catches thrown ring-swagger validation errors turning them
   into valid error respones. Accepts the following options:

   :error-handler - a function of schema.utils.ErrorContainer -> response
   :catch-core-errors? - consume also :schema.core/errors (defaults to false)"
  [handler & [{:keys [error-handler catch-core-errors?]}]]
  (let [error-handler (or error-handler default-error-handler)]
    (fn [request]
      (try+
        (handler request)
        (catch [:type :schema.core/error] validation-error
          (if catch-core-errors?
            (error-handler validation-error)
            (throw+ validation-error)))
        (catch [:type :ring.swagger.schema/validation] error-container
          (error-handler error-container))))))
