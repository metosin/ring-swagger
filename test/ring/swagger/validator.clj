(ns ring.swagger.validator
  (:require [clojure.java.io :as io]
            [scjsv.core :as v]))

;; "http://swagger.io/v2/schema.json"
(def validate
  (v/validator (slurp (io/resource "ring/swagger/v2.0_schema.json"))))
