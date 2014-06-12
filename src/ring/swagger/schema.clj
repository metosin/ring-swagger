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
;; Internals
;;

(defn- collection-with-one-element [x y]
  (assert (= (count x) 1) "nested sequences and set can only one element.")
  (cond
    (set? x) #{y}
    (sequential? x) [y]))

(defn- plain-map? [x]
  (and (instance? clojure.lang.APersistentMap x) ;; need to filter out Schema records
       (not (s/schema-name x)))) ;; and predefined models

(defn- valid-container? [x]
  (or (sequential? x)
      (set? x)))

(defn- sub-model-symbol [model k]
  (symbol (str model (->CamelCase (name (s/explicit-schema-key k))))))

(defn extract-schema-name
  "Returns model name or nil"
  [x] (some-> (if (or (set? x) (sequential? x)) (first x) x) s/schema-name))

;;
;; Public Api
;;

(defmacro defmodel
  "Defines a new Schema model (a Map) and attaches the model var
   to it's metadata - used in handling Model references. Generates
   submodels from direct nested Maps and Maps as only element in
   valid containers (List, Vector, Set) and links them by reference.
   Submodels are named after their father appended with the key name."
  ([model form]
    `(defmodel ~model ~(str model " (Model)\n\n" (let [w (StringWriter.)] (pprint/pprint form w)(.toString w))) ~form))
  ([model docstring form]
    (letfn [(sub-models! [model form]
                         (into {}
                               (for [[k v] form
                                     :let [v (cond

                                               ;; direct anonymous map
                                               (plain-map? v)
                                               (let [sub-model (sub-model-symbol model k)]
                                                 (eval `(defmodel ~sub-model ~v))
                                                 (value-of sub-model))

                                               ;; anonymous map within a valid container
                                               (and (valid-container? v) (plain-map? (first v)))
                                               (let [sub-model (sub-model-symbol model k)]
                                                 (eval `(defmodel ~sub-model ~(first v)))
                                                 (collection-with-one-element v (value-of sub-model)))

                                               ;; pass-through
                                               :else v)]]
                                 [k v])))]
      `(do
         (assert (map? ~form))
         (def ~model ~docstring
           (with-meta
             (~sub-models! '~model ~form)
             {:name '~model}))))))

(defn field
  "Defines a Schema predicate and attaches meta-data into it.
   Supports also Java classes via type-mapping. These include:
   Long, Double, String, Boolean, Date, DateTime and LocalDate."
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

