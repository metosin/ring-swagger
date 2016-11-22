(ns ring.swagger.download
  (:require [ring.swagger.json-schema :as js]
            [schema.core :as s])
  (:import [java.io File]))

(defrecord FileResponse* []

  s/Schema
  (spec [_]
    (s/spec s/Any))
  (explain [_]
    '(file-response))

  js/JsonSchema
  (convert [_ _]
    {:type "file"}))

(def FileResponse (->FileResponse*))
