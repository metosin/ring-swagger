(ns ring.swagger.schema
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [schema.macros :as sm]
            [schema.utils :as su]
            [ring.swagger.common :refer :all]))

;;
;; Primitives
;;

(def Int*      s/Int)
(def Long*     (s/pred (partial instance? Long) 'long?))
(def Float*    (s/pred float? 'float?))
(def Double*   (s/pred (partial instance? Double) 'double?))
(def Str*      (s/pred string? 'string?))
(def Byte*     (s/pred (partial instance? Byte) 'byte?))
(def Boolean*  (s/pred (partial instance? Boolean) 'boolean?))
(def Date*     (s/pred (partial instance? java.util.Date) 'date?))
(def DateTime* (s/pred (partial instance? org.joda.time.DateTime) 'date-time?))

;;
;;
;;

(defn field [pred metadata]
  (let [pred (if (= s/Str pred) Str* pred)
        old-meta (meta pred)]
    (with-meta pred (merge old-meta metadata))))

(defn coerce [model value]
  ((sc/coercer (value-of model) sc/json-coercion-matcher) value))

(defn coerce! [model value]
  (let [result (coerce model value)]
    (if (su/error? result)
      (throw (ex-info (str (:error result)) result))
      result)))

(defmacro defmodel [model form]
  `(def ~model ~(str model) (with-meta ~form {:model (var ~model)})))

(defn model? [x] (and (map? x) (var? (:model (meta x)))))

(defn model-of [x]
  (let [value (value-of x)]
    (if (model? value)
      (:model (meta value)))))

(defn schema-name [x] (some-> x model-of name-of))

(defn enum? [x] (= (class x) schema.core.EnumSchema))
