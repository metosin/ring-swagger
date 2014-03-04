(ns ring.swagger.schema
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [schema.utils :as su]
            [clojure.pprint :as pprint]
            [slingshot.slingshot :refer [throw+]]
            [ring.swagger.common :refer :all]
            [ring.swagger.data :refer :all]
            [camel-snake-kebab :refer [->CamelCase]]
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
               LocalDate Date*
               clojure.lang.Keyword Keyword*})

;;
;; Public Api
;;

(defn model?
  "Checks weather input is a model."
  [x] (and (map? x) (var? (:model (meta x)))))

(defmacro defmodel
  "Defines a new Schema model (a Map) and attaches the model var
   to it's metadata - used in handling Model references. Generates
   submodels from nested Maps and links them by reference. Submodels
   are named after their father and the child key."
  ([model form]
    `(defmodel ~model ~(str model " (Model)\n\n" (let [w (StringWriter.)] (pprint/pprint form w)(.toString w))) ~form))
  ([model docstring form]
    (letfn [(sub-models! [model form]
              (into {}
                (for [[k v] form
                      :let [v (if (and (instance? clojure.lang.APersistentMap v) ;; need to filter out Schema records
                                    (not (model? v))) ;; and predefined models
                                (let [sub-model (symbol (str model (->CamelCase (name (s/explicit-schema-key k)))))]
                                  (eval `(defmodel ~sub-model ~v))
                                  (value-of sub-model))
                                v)]]
                  [k v])))]
      `(do
         (assert (map? ~form))
         (def ~model ~docstring
         (with-meta
           (~sub-models! '~model ~form)
           {:model (var ~model)}))))))

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
