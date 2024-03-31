(ns ring.openapi.validator
  (:require [clojure.java.io :as io]
            [scjsv.core :as v]))

; https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/schemas/v2.0/schema.json
; http://json-schema.org/draft-04/schema

(def validate
  (v/validator (slurp (io/resource "ring/openapi/openapi-schema.json"))))
