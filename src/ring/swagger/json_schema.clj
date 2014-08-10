(ns ring.swagger.json-schema
  (:require [schema.core :as s]))

(defn json-schema-meta
  "Select interesting keys from meta-data of schema."
  [schema]
  (:json-schema (meta schema)))

(defn ->parameter [base json]
  (merge {:description ""
          :required true}
         base
         json))

;;
;; Schema implementation which is used wrap stuff which doesn't support meta-data
;;

(defn field
  "Attaches meta-data to a schema under :json-schema key. If the
   schema is of type which cannot have meta-data (e.g. Java Classes)
   schema is wrapped first into s/both Schema."
  [schema meta-data]
  (with-meta (if (instance? clojure.lang.IObj schema)
               schema
               (s/both schema))
             (merge (meta schema) {:json-schema meta-data})))

(defn describe
  "Attach description and possibly other meta-data to a schema."
  [schema desc & kvs]
  (field schema (merge {:description desc} (apply hash-map kvs))))

;;
;; Describe Java and Clojure classes and Schemas as Json schema
;;

(declare json-type)

(defn ->json
  [x & {:keys [top] :or {top false}}]
  (if-let [json (if top
                  (if-let [schema-name (s/schema-name x)]
                    {:type schema-name}
                    (or (json-type x) {:type "void"}))
                  (json-type x))]
    (merge json (json-schema-meta x))))

(defmulti json-type
  (fn [e]
    (if (instance? java.lang.Class e)
      e
      (class e))))

(defmethod json-type nil [_] {:type "void"})

;; Collections
(defmethod json-type clojure.lang.Sequential [e]
  {:type "array"
   :items (json-type (first e))})
(defmethod json-type clojure.lang.IPersistentSet [e]
  {:type "array"
   :uniqueItems true
   :items (json-type (first e))})

;; Classes
(defmethod json-type java.lang.Long          [_] {:type "integer" :format "int64"})
(defmethod json-type java.lang.Double        [_] {:type "number" :format "double"})
(defmethod json-type java.lang.String        [_] {:type "string"})
(defmethod json-type java.lang.Boolean       [_] {:type "boolean"})
(defmethod json-type clojure.lang.Keyword    [_] {:type "string"})
(defmethod json-type java.util.UUID          [_] {:type "string" :format "uuid"})
(defmethod json-type java.util.Date          [_] {:type "string" :format "date-time"})
(defmethod json-type org.joda.time.DateTime  [_] {:type "string" :format "date-time"})
(defmethod json-type org.joda.time.LocalDate [_] {:type "string" :format "date"})

;; Schemas
;; Convert the most common predicates by mapping fn to Class
(def predicate-to-class {integer? java.lang.Long
                         keyword? clojure.lang.Keyword
                         symbol?  clojure.lang.Symbol})
(defmethod json-type schema.core.Predicate  [e] (if-let [c (predicate-to-class (:p? e))]
                                                  (->json c)))
(defmethod json-type schema.core.EnumSchema [e] (merge (->json (class (first (:vs e)))) {:enum (seq (:vs e))}))
(defmethod json-type schema.core.Maybe      [e] (->json (:schema e)))
(defmethod json-type schema.core.Both       [e] (->json (first (:schemas e))))
(defmethod json-type schema.core.Recursive  [e] (->json (:derefable e)))
(defmethod json-type schema.core.EqSchema   [e] (->json (class (:v e))))
(defmethod json-type schema.core.AnythingSchema [_] nil)

(defmethod json-type :default [e]
  (if (s/schema-name e)
    {:$ref (s/schema-name e)}
    (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e)))))

;;
;; Schema -> Json Schema
;;

(defn not-predicate? [x]
  (not= (class x) schema.core.Predicate))

(defn properties [schema]
  (into {}
        (for [[k v] schema
              :when (not-predicate? k)
              :let [k (s/explicit-schema-key k)
                    v (try (->json v)
                           (catch Exception e
                             (throw
                               (IllegalArgumentException.
                                 (str "error converting to json schema [" k " " (s/explain v) "]") e))))]]
          (and v [k v]))))
