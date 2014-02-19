(ns ring.swagger.schema
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [schema.utils :as su]
            [slingshot.slingshot :refer [throw+]]
            [ring.swagger.common :refer :all]
            [ring.swagger.data :refer :all]
            [ring.swagger.coerce :as coerce])
  (:import  [java.util Date]
            [org.joda.time DateTime LocalDate]))

(def Keyword  s/Keyword)

(def type-map {Long      Long*
               Double    Double*
               String    String*
               Boolean   Boolean*
               Date      DateTime*
               DateTime  DateTime*
               LocalDate Date*
               ;; schema types
               s/Int     Long*})

;;
;; Public Api
;;

(defn field [pred metadata]
  (let [pred (or (type-map pred) pred)
        old-meta (meta pred)]
    (with-meta pred (merge old-meta metadata))))

(defn coerce [model value]
  ((sc/coercer (value-of model) coerce/json-schema-coercion-matcher) value))

(defn coerce! [model value]
  (let [result (coerce model value)]
    (if (su/error? result)
      (throw+ {:type ::validation :error (:error result)})
      result)))

(defmacro defmodel [model form]
  `(def ~model ~(str model) (with-meta ~form {:model (var ~model)})))

(defn model? [x] (and (map? x) (var? (:model (meta x)))))

(defn model-of [x]
  (let [value (value-of x)]
    (if (model? value)
      (:model (meta value)))))

(defn schema-name [x] (some-> x model-of name-of))
