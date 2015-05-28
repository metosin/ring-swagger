(ns ring.swagger.json-schema
  (:require [schema.core :as s]
            [ring.swagger.common :refer [plain-map?]]
            [flatland.ordered.map :refer :all]))

; TODO: clean all 1.2 hacks after Compojure-api goes 2.0

(def ^:dynamic *ignore-missing-mappings* false)
(def ^:dynamic *swagger-spec-version* "1.2")

(defn json-schema-meta
  "Select interesting keys from meta-data of schema."
  [schema]
  (:json-schema (meta schema)))

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

(defn ensure-swagger12-top [schema]
  (if (or false (= *swagger-spec-version* "1.2"))
    (if-let [ref (:$ref schema)]
      (-> schema
          (dissoc :$ref)
          (assoc :type ref))
      schema)
    schema))

(defn ->json
  [x & {:keys [top] :or {top false}}]
  (if-let [json (if top
                  (if-let [schema-name (s/schema-name x)]
                    {:type schema-name}
                    (or (ensure-swagger12-top (json-type x)) {:type "void"}))
                  (json-type x))]
    (merge json (json-schema-meta x))))

(defmulti json-type
  (fn [e]
    (if (instance? Class e)
      e
      (class e))))

(defmethod json-type nil [_] {:type "void"})

;; Collections
(defmethod json-type clojure.lang.Sequential [e]
  {:type "array"
   :items (json-type (first e))
   :collectionFormat "multi"})
(defmethod json-type clojure.lang.IPersistentSet [e]
  {:type "array"
   :uniqueItems true
   :items (json-type (first e))
   :collectionFormat "multi"})

;; Classes
(defmethod json-type java.lang.Integer       [_] {:type "integer" :format "int32"})
(defmethod json-type java.lang.Long          [_] {:type "integer" :format "int64"})
(defmethod json-type java.lang.Double        [_] {:type "number" :format "double"})
(defmethod json-type java.lang.Number        [_] {:type "number" :format "double"})
(defmethod json-type java.lang.String        [_] {:type "string"})
(defmethod json-type java.lang.Boolean       [_] {:type "boolean"})
(defmethod json-type clojure.lang.Keyword    [_] {:type "string"})
(defmethod json-type java.util.UUID          [_] {:type "string" :format "uuid"})
(defmethod json-type java.util.Date          [_] {:type "string" :format "date-time"})
(defmethod json-type org.joda.time.DateTime  [_] {:type "string" :format "date-time"})
(defmethod json-type org.joda.time.LocalDate [_] {:type "string" :format "date"})
(defmethod json-type java.util.regex.Pattern [e]
  (if (instance? java.util.regex.Pattern e)
    {:type "string" :pattern (str e)}
    {:type "string" :format "regex"}))

;; Schemas
;; Convert the most common predicates by mapping fn to Class
(def predicate-to-class {integer? java.lang.Long
                         keyword? clojure.lang.Keyword
                         symbol?  clojure.lang.Symbol})

(defmethod json-type schema.core.Predicate      [e] (if-let [c (predicate-to-class (:p? e))] (->json c)))
(defmethod json-type schema.core.EnumSchema     [e] (merge (->json (class (first (:vs e)))) {:enum (seq (:vs e))}))
(defmethod json-type schema.core.Maybe          [e] (->json (:schema e)))
(defmethod json-type schema.core.Both           [e] (->json (first (:schemas e))))
(defmethod json-type schema.core.Either         [e] (->json (first (:schemas e))))
(defmethod json-type schema.core.Recursive      [e] (->json (:derefable e)))
(defmethod json-type schema.core.EqSchema       [e] (->json (class (:v e))))
(defmethod json-type schema.core.NamedSchema    [e] (->json (:schema e)))
(defmethod json-type schema.core.One            [e] (->json (:schema e)))
(defmethod json-type schema.core.AnythingSchema [_] nil)

(defmethod json-type :default [e]
  (if-let [schema-name (s/schema-name e)]
    (case *swagger-spec-version*
      "1.2" {:$ref schema-name}
      "2.0" {:$ref (str "#/definitions/" schema-name)})
    (and (not *ignore-missing-mappings*)
         (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e))))))

;;
;; Schema -> Json Schema
;;

(defn predicate? [x]
  (= (class x) schema.core.Predicate))

(def not-predicate? (complement predicate?))

(defn try->json [v k]
  (try (->json v)
       (catch Exception e
         (throw
           (IllegalArgumentException.
             (str "error converting to json schema [" k " " (s/explain v) "]") e)))))

(defn properties
  "Take a map schema and turn them into json-schema properties.
   The result is put into collection of same type as input schema.
   Thus ordered-map should keep the order of items."
  [schema]
  {:pre [(plain-map? schema)]}
  (into (empty schema)
        (for [[k v] schema
              :when (not-predicate? k)
              :let [k (s/explicit-schema-key k)
                    v (try->json v k)]]
          (and v [k v]))))

(defn additional-properties
  "Generates json-schema additional properties from a plain map
  schema from under key s/Keyword."
  [schema]
  {:pre [(plain-map? schema)]}
  (if-let [v (schema s/Keyword)]
    (try->json v s/Keyword)))
