(ns ring.swagger.schema
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [schema.utils :as su]
            [potemkin :refer [import-vars]]
            [clojure.pprint :as pprint]
            [slingshot.slingshot :refer [throw+]]
            [org.tobereplaced.lettercase :as lc]
            [ring.swagger.common :refer :all]
            [ring.swagger.impl :refer :all]
            [ring.swagger.coerce :as coerce]
            ring.swagger.json-schema)
  (:import  [java.io StringWriter]
            [java.util Date UUID]
            [org.joda.time DateTime LocalDate]))

(def Keyword  s/Keyword)

;;
;; Internals
;;

(defn- plain-map? [x]
  (and (instance? clojure.lang.APersistentMap x) ;; need to filter out Schema records
       (not (s/schema-name x)))) ;; and predefined models

(defn- sub-model-symbol [model k]
  (symbol (str model (lc/capitalized (name (s/explicit-schema-key k))))))

(defn extract-schema-name
  "Returns model name or nil"
  [x] (some-> (if (or (set? x) (sequential? x)) (first x) x) s/schema-name))

;;
;; Public Api
;;

(import-vars [ring.swagger.json-schema
              field
              describe])

(defn error?
  "Checks whether input is an Schema error."
  [x] (su/error? x))

(defn named-schema?
  "Checks whether input is a named schema."
  [x] (boolean (s/schema-name x)))

(defn coerce
  "Coerces a value against a schema using enhanced json-coercion.
   If no errors, returns the value, otherwise returns ValidationError."
  ([model value] (coerce model value :json))
  ([model value type]
    ((sc/coercer (value-of model) (coerce/coercer type)) value)))

(defn coerce!
  "Coerces a value against a schema using enhanced json-coercion.
   If no errors, returns the value, otherwise slingshots a
   validation exception."
  ([model value] (coerce! model value :json))
  ([model value type]
    (let [result (coerce model value type)]
      (if (error? result)
        (throw+ {:type ::validation :error (:error result)})
        result))))
