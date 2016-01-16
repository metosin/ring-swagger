(ns ring.swagger.validator
  (:require [clojure.java.io :as io]
            [scjsv.core :as v]))

; https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/schemas/v2.0/schema.json
; http://json-schema.org/draft-04/schema

; TODO: how to reference to a local copy of the ring/swagger/json-schema.json?
(def validate
  (v/validator (slurp (io/resource "ring/swagger/swagger-schema.json"))))
