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

(defn deep-merge-swagger-data-to-request
  "Deep-merges top-level swagger-data into a request.
  Can be read with get-meta-from-request."
  [request data]
  (update-in request [::data] deep-merge data))

(defn get-swagger-data-from-request
  "Reads top-level swagger-data from request, pushed in by
  deep-merge-meta-to-request."
  [request]
  (::data request))

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
