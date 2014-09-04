(ns ring.swagger.json-schema-dirty
  (:require [schema.core :as s]
            [ring.swagger.json-schema :refer :all]))

(defmethod json-type schema.core.ConditionalSchema [e]
  {:type "void" :oneOf (mapv (comp ->json second) (:preds-and-schemas e))})

(defmethod json-type schema.core.Either [e]
  {:type "void" :oneOf (mapv ->json (:schemas e))})
