(ns ring.swagger.json-schema-dirty
  "Json-type multimethod implementations for some Schemas which can't be
   properly described using Json Schema."
  (:require [ring.swagger.json-schema :refer :all]))

(defmethod json-type schema.core.ConditionalSchema [e]
  {:type "void" :oneOf (mapv (comp ->json second) (:preds-and-schemas e))})
