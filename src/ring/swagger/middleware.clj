(ns ring.swagger.middleware
  (:require [slingshot.slingshot :refer [try+]]
            [schema.utils :as su]
            [plumbing.core :refer [for-map]]
            [clojure.walk :refer :all]
            [ring.util.http-response :refer [bad-request]])
  (:import (schema.utils ValidationError)))

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

(defn default-error-handler [error]
  (bad-request {:errors (stringify-error error)}))

(defn catch-validation-errors
  "Catches thrown ring-swagger validation errors and turns them into valid
   error respones. Accepts the following options:

   :error-handler - a function of schema.utils.ErrorContainer -> response"
  [handler & {:keys [error-handler] :or {error-handler default-error-handler}}]
  (fn [request]
    (try+
      (handler request)
      (catch [:type :ring.swagger.schema/validation] {:keys [error]}
        (error-handler error)))))
