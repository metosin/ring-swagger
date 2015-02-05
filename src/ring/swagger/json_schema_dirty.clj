(ns ring.swagger.json-schema-dirty
  (:require [ring.swagger.json-schema :refer :all]))

(defmethod json-type schema.core.ConditionalSchema [e]
  {:type "void" :oneOf (mapv (comp ->json second) (:preds-and-schemas e))})
