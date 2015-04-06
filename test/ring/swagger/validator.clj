(ns ring.swagger.validator
  (:require [clojure.java.io :as io]
            [scjsv.core :as v]))

;; "http://swagger.io/v2/schema.json"
(def schema-string
  (delay (slurp (io/file (io/resource "ring/swagger/v2.0_schema.json")))))

(defn validate [data] (v/validate @schema-string data))
