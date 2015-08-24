(ns ring.swagger.json-schema
  (:require [schema.core :as s]
            [ring.swagger.common :refer [plain-map?]]
            [flatland.ordered.map :refer :all]))

(def ^:dynamic *ignore-missing-mappings* false)

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

(defmulti to-json-property (fn [c options] c))

(defprotocol JsonSchema
  (json-property [this options]))

(defn assoc-collection-format
  "Add collectionFormat to the JSON Schema if the parameter type
   is query or formData."
  [m options]
  (if (#{:query :formData} (:in options))
    (assoc m :collectionFormat (:collection-format options "multi"))
    m))

(defn merge-meta
  [m x {no-meta ::no-meta}]
  (if-not no-meta
    (merge (json-schema-meta x) m)
    m))

(defn not-supported! [e]
  (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e))))

;; Classes
(defmethod to-json-property java.lang.Integer       [_ _] {:type "integer" :format "int32"})
(defmethod to-json-property java.lang.Long          [_ _] {:type "integer" :format "int64"})
(defmethod to-json-property java.lang.Double        [_ _] {:type "number" :format "double"})
(defmethod to-json-property java.lang.Number        [_ _] {:type "number" :format "double"})
(defmethod to-json-property java.lang.String        [_ _] {:type "string"})
(defmethod to-json-property java.lang.Boolean       [_ _] {:type "boolean"})
(defmethod to-json-property clojure.lang.Keyword    [_ _] {:type "string"})
(defmethod to-json-property java.util.UUID          [_ _] {:type "string" :format "uuid"})
(defmethod to-json-property java.util.Date          [_ _] {:type "string" :format "date-time"})
(defmethod to-json-property org.joda.time.DateTime  [_ _] {:type "string" :format "date-time"})
(defmethod to-json-property org.joda.time.LocalDate [_ _] {:type "string" :format "date"})
(defmethod to-json-property java.util.regex.Pattern [_ _] {:type "string" :format "regex"})

(defmethod to-json-property :default [e _]
  (if-not *ignore-missing-mappings*
    (not-supported! e)))

;; Schemas
;; Convert the most common predicates by mapping fn to Class
(def predicate-to-class {integer? java.lang.Long
                         keyword? clojure.lang.Keyword
                         symbol?  clojure.lang.Symbol})

(defn- reference [e]
  (if-let [schema-name (s/schema-name e)]
    {:$ref (str "#/definitions/" schema-name)}
    (and (not *ignore-missing-mappings*)
         (not-supported! e))))

(defn ->json
  ([x] (->json x {}))
  ([x options]
   (merge-meta (json-property x options) x options)))

(defn- coll-schema [e options]
  (-> {:type "array"
       :items (->json (first e) (assoc options ::no-meta true))}
      (assoc-collection-format options)))

(extend-protocol JsonSchema
  Object
  (json-property [e _] (not-supported! e))

  Class
  (json-property [e options]
    (to-json-property e options))

  nil
  (json-property [_ _] {:type "void"})

  schema.core.Predicate
  (json-property [e _] (some-> e :p? predicate-to-class ->json))

  schema.core.EnumSchema
  (json-property [e _] (merge (->json (class (first (:vs e)))) {:enum (seq (:vs e))}))

  schema.core.Maybe
  (json-property [e {:keys [in]}]
    (let [schema (->json (:schema e))]
      (if (#{:query :formData} in)
        (assoc schema :allowEmptyValue true)
        schema)))

  schema.core.Both
  (json-property [e _] (->json (first (:schemas e))))

  schema.core.Either
  (json-property [e _] (->json (first (:schemas e))))

  schema.core.Recursive
  (json-property [e _] (->json (:derefable e)))

  schema.core.EqSchema
  (json-property [e _] (->json (class (:v e))))

  schema.core.NamedSchema
  (json-property [e _] (->json (:schema e)))

  schema.core.One
  (json-property [e _] (->json (:schema e)))

  schema.core.AnythingSchema
  (json-property [_ _] nil)

  java.util.regex.Pattern
  (json-property [e _] {:type "string" :pattern (str e)})

  ;; Collections
  clojure.lang.Sequential
  (json-property [e options] (coll-schema e options))

  clojure.lang.IPersistentSet
  (json-property [e options] (assoc (coll-schema e options) :uniqueItems true))

  clojure.lang.IPersistentMap
  (json-property [e _] (reference e))

  clojure.lang.Var
  (json-property [e _] (reference e)))

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
   Thus ordered-map should keep the order of items. Returnes nil
   if no properties are found."
  [schema]
  {:pre [(plain-map? schema)]}
  (let [props (into (empty schema)
                    (for [[k v] schema
                          :when (not-predicate? k)
                          :let [k (s/explicit-schema-key k)
                                v (try->json v k)]]
                      (and v [k v])))]
    (if (seq props)
      props)))

(defn additional-properties
  "Generates json-schema additional properties from a plain map
  schema from under key s/Keyword."
  [schema]
  {:pre [(plain-map? schema)]}
  (if-let [v (schema s/Keyword)]
    (try->json v s/Keyword)))
