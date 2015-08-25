(ns ring.swagger.json-schema-dirty
  "JsonSchema conversions for some Schemas which can't be
  properly described using the Swagger Schema."
  (:require [ring.swagger.json-schema :refer :all]))

(extend-protocol JsonSchema
  schema.core.ConditionalSchema
  (convert [e _]
    {:type "void" :oneOf (mapv (comp ->swagger second) (:preds-and-schemas e))}))
