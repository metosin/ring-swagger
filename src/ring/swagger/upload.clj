(ns ring.swagger.upload
  (:require [potemkin]
            [ring.swagger.json-schema :as js]
            [schema.core :as s])
  (:import [java.io File]))

; Works exactly like map schema but wrapped in record for JsonSchema dispatch
(defrecord Upload [m]

  s/Schema
  (spec [_]
    (s/spec m))
  (explain [_]
    (cons 'file m))

  js/JsonSchema
  (convert [_ _]
    {:type "file"}))

(def TempFileUpload
  "Schema for file param created by ring.middleware.multipart-params.temp-file store."
  (->Upload {:filename s/Str
             :content-type s/Str
             :size s/Int
             (s/optional-key :tempfile) File}))

(def ByteArrayUpload
  "Schema for file param created by ring.middleware.multipart-params.byte-array store."
  (->Upload {:filename s/Str
             :content-type s/Str
             :bytes s/Any}))
