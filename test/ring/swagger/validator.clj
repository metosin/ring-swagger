(ns ring.swagger.validator
  (:import [com.fasterxml.jackson.databind ObjectMapper]
           [com.github.fge.jsonschema.main JsonSchemaFactory]
           [com.github.fge.jackson JsonLoader]
           [com.github.fge.jsonschema.core.report ListProcessingReport]))

(defonce schema-string (delay (slurp "http://swagger.io/v2/schema.json")))

(defn validate [input-doc]
  (let [mapper        (ObjectMapper.)
        schema-object (.readTree mapper @schema-string)
        factory       (JsonSchemaFactory/byDefault)
        schema        (.getJsonSchema factory schema-object)
        report        (.validate schema (JsonLoader/fromString input-doc))
        lp            (ListProcessingReport.)
        _             (.mergeWith lp report)
        errors        (iterator-seq (.iterator lp))]
    (if (seq errors)
      (map str errors))))
