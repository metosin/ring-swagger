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

(defmulti json-type identity)

(defprotocol JsonSchema
  (json-property [this])
  (json-parameter [this]))

(defn ensure-swagger12-top [schema]
  (if (or false (= *swagger-spec-version* "1.2"))
    (if-let [ref (:$ref schema)]
      (-> schema
          (dissoc :$ref)
          (assoc :type ref))
      schema)
    schema))

(defn ->json-schema [x & {:keys [type]
                          :or {type :property}}]
  (if (instance? Class x)
    (json-type x)
    (case type
      :parameter (json-parameter x)
      :property  (json-property x))))

(defn ->json
  [x & {:keys [top type no-meta]
        :or {top false
             type :property}}]
  (if-let [json (if top
                  (if-let [schema-name (s/schema-name x)]
                    {:type schema-name}
                    (or (ensure-swagger12-top (->json-schema x :type type))
                        {:type "void"}))
                  (->json-schema x :type type))]
    (cond->> json
      (not no-meta) (merge (json-schema-meta x)))))

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
(defmethod json-type java.util.regex.Pattern [e] {:type "string" :format "regex"})

(defmethod json-type :default [e]
  (if-not *ignore-missing-mappings*
    (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e)))))

;; Schemas
;; Convert the most common predicates by mapping fn to Class
(def predicate-to-class {integer? java.lang.Long
                         keyword? clojure.lang.Keyword
                         symbol?  clojure.lang.Symbol})

(extend-protocol JsonSchema
  Object
  (json-property [e] (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e))))
  (json-parameter [e] (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e))))

  nil
  (json-property [_] {:type "void"})
  (json-parameter [_] {:type "void"})

  schema.core.Predicate
  (json-property [e] (some-> e :p? predicate-to-class ->json))
  (json-parameter [e] (some-> e :p? predicate-to-class ->json))

  schema.core.EnumSchema
  (json-property [e] (merge (->json (class (first (:vs e)))) {:enum (seq (:vs e))}))
  (json-parameter [e] (merge (->json (class (first (:vs e)))) {:enum (seq (:vs e))}))

  schema.core.Maybe
  (json-property [e] (->json (:schema e)))
  (json-parameter [e] (->json (:schema e)))

  schema.core.Both
  (json-property [e] (->json (first (:schemas e))))
  (json-parameter [e] (->json (first (:schemas e))))

  schema.core.Either
  (json-property [e] (->json (first (:schemas e))))
  (json-parameter [e] (->json (first (:schemas e))))

  schema.core.Recursive
  (json-property [e] (->json (:derefable e)))
  (json-parameter [e] (->json (:derefable e)))

  schema.core.EqSchema
  (json-property [e] (->json (class (:v e))))
  (json-parameter [e] (->json (class (:v e))))

  schema.core.NamedSchema
  (json-property [e] (->json (:schema e)))
  (json-parameter [e] (->json (:schema e)))

  schema.core.One
  (json-property [e] (->json (:schema e)))
  (json-parameter [e] (->json (:schema e)))

  schema.core.AnythingSchema
  (json-property [_] nil)
  (json-parameter [_] nil)

  java.util.regex.Pattern
  (json-property [e] {:type "string" :pattern (str e)})
  (json-parameter [e] {:type "string" :pattern (str e)})

  ;; Collections
  clojure.lang.Sequential
  (json-property [e]
    {:type "array"
     :items (->json (first e) :no-meta true)})
  (json-parameter [e]
    {:type "array"
     :items (->json (first e) :no-meta true)
     :collectionFormat "multi"})

  clojure.lang.IPersistentSet
  (json-property [e]
    {:type "array"
     :uniqueItems true
     :items (->json (first e) :no-meta true)})
  (json-parameter [e]
    {:type "array"
     :uniqueItems true
     :items (->json (first e) :no-meta true)
     :collectionFormat "multi"})

  clojure.lang.IPersistentMap
  (json-property [e]
    (if-let [schema-name (s/schema-name e)]
      (case *swagger-spec-version*
        "1.2" {:$ref schema-name}
        "2.0" {:$ref (str "#/definitions/" schema-name)})
      (and (not *ignore-missing-mappings*)
           (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e))))))
  (json-parameter [e]
    (if-let [schema-name (s/schema-name e)]
      (case *swagger-spec-version*
        "1.2" {:$ref schema-name}
        "2.0" {:$ref (str "#/definitions/" schema-name)})
      (and (not *ignore-missing-mappings*)
           (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e))))))

  clojure.lang.Var
  (json-property [e]
    (if-let [schema-name (s/schema-name e)]
      (case *swagger-spec-version*
        "1.2" {:$ref schema-name}
        "2.0" {:$ref (str "#/definitions/" schema-name)})
      (and (not *ignore-missing-mappings*)
           (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e))))))
  (json-parameter [e]
    (if-let [schema-name (s/schema-name e)]
      (case *swagger-spec-version*
        "1.2" {:$ref schema-name}
        "2.0" {:$ref (str "#/definitions/" schema-name)})
      (and (not *ignore-missing-mappings*)
           (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e)))))))

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
