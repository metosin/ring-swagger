(ns ring.swagger.schema
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [schema.utils :as su]
            [clojure.pprint :as pprint]
            [slingshot.slingshot :refer [throw+]]
            [ring.swagger.common :refer :all]
            [ring.swagger.data :refer :all]
            [ring.swagger.coerce :as coerce])
  (:import  [java.io StringWriter]
            [java.util Date]
            [org.joda.time DateTime LocalDate]))

(def Keyword  s/Keyword)

(def type-map {Long      Long*
               Double    Double*
               String    String*
               Boolean   Boolean*
               Date      DateTime*
               DateTime  DateTime*
               LocalDate Date*})

;;
;; Public Api
;;

(defmacro defmodel
  "Defines a new Schema model (a Map) and attaches the model var
   and the name of the model to it's metadata - used in handling
   Model references."
  ([name form]
    `(defmodel ~name ~(str name " (Model)\n\n" (let [w (StringWriter.)] (pprint/pprint form w)(.toString w))) ~form))
  ([name docstring form]
    {:pre  [(map? form)]}
    `(def ~name ~docstring (with-meta ~form {:model (var ~name)
                                             :name  '~name}))))

(defn model?
  "Checks weather input is a model."
  [x] (and (map? x) (var? (:model (meta x)))))

(defn field
  "Defines a Schema predicate and attaches meta-data into it.
   Supports also basic immutable Java objects via type-mapping.
   These include: Long, Double, String, Boolean, Date, DateTime
   and LocalDate."
  [pred metadata]
  (let [pred (or (type-map pred) pred)
        old-meta (meta pred)]
    (with-meta pred (merge old-meta metadata))))

(defn error?
  "Checks weather input is an Schema error."
  [x] (su/error? x))

(defn coerce
  "Coerces a value against a schema using enhanced json-coercion.
   If no errors, returns the value, otherwise returns ValidationError."
  [model value]
  ((sc/coercer (value-of model) coerce/json-schema-coercion-matcher) value))

(defn coerce! [model value]
  "Coerces a value against a schema using enhanced json-coercion.
   If no errors, returns the value, otherwise slingshots a
   validation exception."
  (let [result (coerce model value)]
    (if (error? result)
      (throw+ {:type ::validation :error (:error result)})
      result)))

(defn model-var
  "Returns models var."
  [x]
  (let [value (value-of x)]
    (if (model? value)
      (:model (meta value)))))

(defn model-name
  "Returns model name or nil"
  [x] (some-> x model-var name-of))
