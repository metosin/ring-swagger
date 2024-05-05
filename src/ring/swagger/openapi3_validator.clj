(ns ring.swagger.openapi3-validator
  (:require [clojure.java.io :as io]
            [scjsv.core :as v]))

; https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/schemas/v3.0/schema.json
; http://json-schema.org/draft-04/schema

(def validate
  (v/validator (slurp (io/resource "ring/swagger/openapi-schema.json"))))
