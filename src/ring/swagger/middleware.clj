(ns ring.swagger.middleware
  (:require [slingshot.slingshot :refer [try+]]
            [schema.utils :as su]
            [clojure.walk :refer :all]
            [ring.util.http-response :refer [bad-request]]))

(defn stringify-error [error]
  (postwalk
    (fn [x]
      (if-not (map? x)
        x
        (into {}
              (for [[k v] x]
                [k (cond
                     (instance? schema.utils.ValidationError v) (str (su/validation-error-explain v))
                     (symbol? v) (str v)
                     :else v)]))))
    error))

(defn catch-validation-errors
  [handler]
  (fn [request]
    (try+
      (handler request)
      (catch [:type :ring.swagger.schema/validation] {:keys [error]}
        (bad-request
          {:errors (stringify-error error)})))))
