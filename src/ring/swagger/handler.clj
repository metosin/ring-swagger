(ns ring.swagger.handler
  (:require [plumbing.fnk.pfnk :as pfnk]
            [schema.core :as s]
            [plumbing.core :refer [fnk for-map]]))

(def ring2swagger
  {:query-params :query
   :path-params :path
   :body-params :body
   :header-params :header})

(defn swagger-parameters [schema]
  {:parameters (for-map [[ring-key swagger-key] ring2swagger
                         :let [ring-value (get-in schema [:request ring-key])]
                         :when ring-value]
                 swagger-key ring-value)})

;;
;; Public API
;;

(defn handler
  ([f]
   (handler {} f))
  ([handler-meta f]
   (let [f-meta (meta f)
         schema (if (:schema f-meta) (pfnk/input-schema f))
         imeta (some-> schema swagger-parameters)
         full-meta (merge imeta handler-meta)]
     (with-meta f full-meta))))
