(ns ring.swagger.json-schema
  (:require [schema.core :as s]
            [schema.spec.core :as spec]
            [schema.spec.variant :as variant]
            [ring.swagger.common :as common]
            [ring.swagger.core :as rsc]
            [ring.swagger.extension :as extension]))

(defn maybe? [schema]
  (instance? schema.core.Maybe schema))

(declare properties)
(declare schema-object)

; TODO: remove this in favor of passing it as options
(def ^:dynamic *ignore-missing-mappings* false)

(defn json-schema-meta
  "Select interesting keys from meta-data of schema."
  [schema] (:json-schema (meta schema)))

;;
;; Schema implementation which is used wrap stuff which doesn't support meta-data
;;

(defrecord FieldSchema [schema]
  schema.core.Schema
  (spec [_]
    (variant/variant-spec
      spec/+no-precondition+
      [{:schema schema}]))
  (explain [_] (s/explain schema)))

(defn field
  "Attaches meta-data to a schema under :json-schema key. If the
  schema is of type which cannot have meta-data (e.g. Java Classes)
  schema is wrapped first into wrapper Schema."
  [schema meta-data]
  (with-meta (if (instance? clojure.lang.IObj schema)
               schema
               (->FieldSchema schema))
             (merge (meta schema) {:json-schema meta-data})))

(defn describe
  "Attach description and possibly other meta-data to a schema."
  [schema desc & kvs]
  (field schema (merge {:description desc} (apply hash-map kvs))))

;;
;; Describe Java and Clojure classes and Schemas as Json schema
;;

(defn key-name [x]
  (if (keyword? x)
    (let [n (namespace x)]
      (str (if n (str n "/")) (name x)))
    x))

(defmulti convert-class (fn [c _ _] c))

(defprotocol JsonSchema
  (convert [this options schema-type]))

(defn not-supported! [e]
  (throw (IllegalArgumentException.
           (str "don't know how to convert " e " into a Swagger Schema. "
                "Check out ring-swagger docs for details."))))

(defn assoc-collection-format
  "Add collectionFormat to the JSON Schema if the parameter type
   is query or formData."
  [m options]
  (if (#{:query :formData} (:in options))
    (assoc m :collectionFormat (:collection-format options "multi"))
    m))

(defn reference? [m]
  (contains? m :$ref))

(defn reference [e schema-type]
  (if-let [schema-name (s/schema-name e)]
    {:$ref (str (if (= schema-type :openapi) "#/components/schemas/" "#/definitions/") schema-name)}
    (if (not *ignore-missing-mappings*)
      (not-supported! e))))

(defn merge-meta
  [m x {:keys [::no-meta :key-meta]}]
  (if (and (not no-meta) (not (reference? m)))
    (merge (json-schema-meta x)
           (if key-meta (common/remove-empty-keys (select-keys key-meta [:default])))
           m)
    m))

;; Classes
(defmethod convert-class java.lang.Integer       [_ _ _] {:type "integer" :format "int32"})
(defmethod convert-class java.lang.Long          [_ _ _] {:type "integer" :format "int64"})
(defmethod convert-class java.lang.Double        [_ _ _] {:type "number" :format "double"})
(defmethod convert-class java.lang.Number        [_ _ _] {:type "number" :format "double"})
(defmethod convert-class java.lang.String        [_ _ _] {:type "string"})
(defmethod convert-class java.lang.Boolean       [_ _ _] {:type "boolean"})
(defmethod convert-class clojure.lang.Keyword    [_ _ _] {:type "string"})
(defmethod convert-class clojure.lang.Symbol     [_ _ _] {:type "string"})
(defmethod convert-class java.util.UUID          [_ _ _] {:type "string" :format "uuid"})
(defmethod convert-class java.util.Date          [_ _ _] {:type "string" :format "date-time"})
(defmethod convert-class org.joda.time.DateTime  [_ _ _] {:type "string" :format "date-time"})
(defmethod convert-class org.joda.time.LocalDate [_ _ _] {:type "string" :format "date"})
(defmethod convert-class org.joda.time.LocalTime [_ _ _] {:type "string" :format "time"})
(defmethod convert-class java.util.regex.Pattern [_ _ _] {:type "string" :format "regex"})
(defmethod convert-class java.io.File            [_ _ _] {:type "file"})

(extension/java-time
 (defmethod convert-class java.time.Instant   [_ _ _] {:type "string" :format "date-time"})
 (defmethod convert-class java.time.LocalDate [_ _ _] {:type "string" :format "date"})
 (defmethod convert-class java.time.LocalTime [_ _ _] {:type "string" :format "time"}))

(defmethod convert-class :default [e _ _]
  (if-not *ignore-missing-mappings*
    (not-supported! e)))

;;
;; Convert the most common predicates by mapping fn to Class
;;

(def predicate-name-to-class {'integer? java.lang.Long
                              'keyword? clojure.lang.Keyword
                              'symbol? clojure.lang.Symbol})

(defn ->swagger
  ([x]
   (->swagger x {} :swagger))
  ([x options schema-type]
   (-> x
       (convert options schema-type)
       (merge-meta x options))))

(defn try->swagger [v k key-meta schema-type]
  (try (->swagger v {:key-meta key-meta} schema-type)
       (catch Exception e
         (throw
          (IllegalArgumentException.
           (str "error converting to swagger schema [" k " "
                (try (s/explain v) (catch Exception _ v)) "]") e)))))


(defn- coll-schema [e options schema-type]
  (-> {:type "array"
       :items (->swagger (first e) (assoc options ::no-meta true) schema-type)}
      (assoc-collection-format options)))

(extend-protocol JsonSchema

  Object
  (convert [e _ _]
    (not-supported! e))

  Class
  (convert [e options schema-type]
    (if-let [schema (common/record-schema e)]
      (schema-object schema schema-type)
      (convert-class e options schema-type)))

  nil
  (convert [_ _ _]
    nil)

  FieldSchema
  (convert [e _ schema-type]
    (->swagger (:schema e) {} schema-type))

  schema.core.Predicate
  (convert [e _ schema-type]
    (some-> e :pred-name predicate-name-to-class (->swagger {} schema-type)))

  schema.core.EnumSchema
  (convert [e options schema-type]
    (merge (->swagger (class (first (:vs e))) options schema-type) {:enum (seq (:vs e))}))

  schema.core.Maybe
  (convert [e {:keys [in]} _]
    (let [schema (->swagger (:schema e))]
      (condp contains? in
        #{:query :formData} (assoc schema :allowEmptyValue true)
        #{nil :body} (assoc schema :x-nullable true)
        schema)))

  schema.core.Both
  (convert [e _ _]
    (->swagger (first (:schemas e))))

  schema.core.Either
  (convert [e _ _]
    (->swagger (first (:schemas e))))

  schema.core.Recursive
  (convert [e _ _]
    (->swagger (:derefable e)))

  schema.core.EqSchema
  (convert [e _ _]
    (merge (->swagger (class (:v e)))
           {:enum [(:v e)]}))

  schema.core.NamedSchema
  (convert [e _ schema-type]
    (->swagger (:schema e) {} schema-type))

  schema.core.One
  (convert [e _ _]
    (->swagger (:schema e)))

  schema.core.AnythingSchema
  (convert [_ {:keys [in] :as opts} schema-type]
    (if (and in (not= :body in))
      (->swagger (s/maybe s/Str) opts schema-type)
      {}))

  schema.core.ConditionalSchema
  (convert [e _ _]
    {:x-oneOf (vec (keep (comp ->swagger second) (:preds-and-schemas e)))})

  schema.core.CondPre
  (convert [e _ _]
    {:x-oneOf (mapv ->swagger (:schemas e))})

  schema.core.Constrained
  (convert [e _ _]
    (->swagger (:schema e)))

  java.util.regex.Pattern
  (convert [e _ _]
    {:type "string" :pattern (str e)})

  ;; Collections

  clojure.lang.Sequential
  (convert [e options schema-type]
    (coll-schema e options schema-type))

  clojure.lang.IPersistentSet
  (convert [e options schema-type]
    (assoc (coll-schema e options schema-type) :uniqueItems true))

  clojure.lang.IPersistentMap
  (convert [e {:keys [properties?]} schema-type]
    (if properties?
      {:properties (properties e schema-type)}
      (reference e schema-type)))

  clojure.lang.Var
  (convert [e _ schema-type]
    (reference e schema-type)))

;;
;; Schema to Swagger Schema definitions
;;

(defn properties
  "Take a map schema and turn them into json-schema properties.
  The result is put into collection of same type as input schema.
  Thus linked/map should keep the order of items. Returnes nil
  if no properties are found."
  [schema schema-type]
  {:pre [(common/plain-map? schema)]}
  (let [props (into (empty schema)
                    (for [[k v] schema
                          :when (s/specific-key? k)
                          :let [key-meta (meta k)
                                k (s/explicit-schema-key k)]
                          :let [v (try->swagger v k key-meta schema-type)]]
                      (and v [k v])))]
    (if (seq props)
      props)))

(defn additional-properties
  "Generates json-schema additional properties from a plain map
  schema from under key s/Keyword."
  [schema schema-type]
  {:pre [(common/plain-map? schema)]}
  (if-let [extra-key (s/find-extra-keys-schema schema)]
    (let [v (get schema extra-key)]
      (try->swagger v s/Keyword nil schema-type ))
    false))

(defn schema-object
  "Returns a JSON Schema object of a plain map schema."
  [schema schema-type]
  (if (common/plain-map? schema)
    (let [properties (properties schema schema-type)
          title (if (not (s/schema-name schema)) (common/title schema))
          additional-properties (additional-properties schema schema-type)
          meta (json-schema-meta schema)
          required (some->> (rsc/required-keys schema)
                            (filter (partial contains? properties))
                            seq
                            vec)]
      (common/remove-empty-keys
        (merge
          meta
          {:type "object"
           :title title
           :properties properties
           :additionalProperties additional-properties
           :required required})))))
