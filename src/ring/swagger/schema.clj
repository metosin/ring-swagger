(ns ring.swagger.schema
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [schema.utils :as su]
            [potemkin]
            [ring.swagger.common :as common]
            [ring.swagger.coerce :as coerce]
            ring.swagger.json-schema))

(potemkin/import-vars [ring.swagger.json-schema
                       field
                       describe])

(defn error?
  "Checks whether input is an Schema error."
  [x] (su/error? x))

(defn named-schema?
  "Checks whether input is a named schema."
  [x] (boolean (s/schema-name x)))

(defn coerce
  "Coerces a value against a schema using a given coerser. If no errors,
   returns the coerced value, otherwise returns ValidationError.

   Optional third parameter is a dispatch value to ring.swagger.coerce/coercer,
   defaults to :json"
  ([schema value] (coerce schema value :json))
  ([schema value type]
   (let [coercer (if (keyword? type) (coerce/coercer type) type)]
     ((sc/coercer (common/value-of schema) coercer) value))))

(defn coerce!
  "Coerces a value against a schema using a given coerser. If no errors,
   returns the coerced value, otherwise throws a schema.utils.ErrorContainer
   enriched with :type ::validation.

   Optional third parameter is a dispatch value to ring.swagger.coerce/coercer,
   defaults to :json"
  ([schema value] (coerce! schema value :json))
  ([schema value type]
   (let [result (coerce schema value type)]
     (if (error? result)
       (throw (ex-info (str "Validation failed") (assoc result :type ::validation)))
       result))))
