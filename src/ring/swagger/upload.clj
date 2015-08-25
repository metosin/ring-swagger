(ns ring.swagger.upload
  (:require [potemkin :refer [import-vars]]
            [ring.middleware.multipart-params]
            [schema.core :as s]))

(import-vars
  [ring.middleware.multipart-params

   wrap-multipart-params])

; Works exactly like map schema but wrapped in record for json-type dispatch
(defrecord Upload [m]

  schema.core.Schema
  (walker [_]
    (let [sub-walker (s/subschema-walker m)]
      (clojure.core/fn [x]
       (if (schema.utils/error? x)
         x
         (sub-walker x)))))
  (explain [_] (cons 'file m))

  ring.swagger.json_schema.JsonSchema
  (convert [_ _]
    {:type "file"}))

(def TempFileUpload
  "Schema for file param created by ring.middleware.multipart-params.temp-file store."
  (->Upload {:filename s/Str
             :content-type s/Str
             :size s/Int
             (s/optional-key :tempfile) java.io.File}))

(def ByteArrayUpload
  "Schema for file param created by ring.middleware.multipart-params.byte-array store."
  (->Upload {:filename s/Str
             :content-type s/Str
             :bytes s/Any}))
