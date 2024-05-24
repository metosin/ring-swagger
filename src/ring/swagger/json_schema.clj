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

(defn- opts->schema-type [opts]
  {:post [(keyword? %)]}
  (get opts :schema-type :swagger))

; TODO: remove this in favor of passing it as options
(def ^:dynamic *ignore-missing-mappings* false)

(defn json-schema-meta
  "Select interesting keys from meta-data of schema."
  [schema] (:json-schema (meta schema)))

;;
;; Schema implementation which is used wrap stuff which doesn't support meta-data
;;

(defrecord FieldSchema [schema]
  s/Schema
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

(defmulti convert-class (fn [c _] c))

(defprotocol JsonSchema
  (convert [this options]))

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

(defn reference
  ([e] (reference e nil))
  ([e opts]
   (if-let [schema-name (s/schema-name e)]
     {:$ref (str (case (opts->schema-type opts)
                   :swagger "#/definitions/"
                   :openapi "#/components/schemas/")
                 schema-name)}
     (if (not *ignore-missing-mappings*)
       (not-supported! e)))))

(defn merge-meta
  [m x {:keys [::no-meta :key-meta]}]
  (if (and (not no-meta) (not (reference? m)))
    (merge (json-schema-meta x)
           (if key-meta (common/remove-empty-keys (select-keys key-meta [:default])))
           m)
    m))

;; Classes
(defmethod convert-class java.lang.Integer       [_ _] {:type "integer" :format "int32"})
(defmethod convert-class java.lang.Long          [_ _] {:type "integer" :format "int64"})
(defmethod convert-class java.lang.Double        [_ _] {:type "number" :format "double"})
(defmethod convert-class java.lang.Number        [_ _] {:type "number" :format "double"})
(defmethod convert-class java.lang.String        [_ _] {:type "string"})
(defmethod convert-class java.lang.Boolean       [_ _] {:type "boolean"})
(defmethod convert-class clojure.lang.Keyword    [_ _] {:type "string"})
(defmethod convert-class clojure.lang.Symbol     [_ _] {:type "string"})
(defmethod convert-class java.util.UUID          [_ _] {:type "string" :format "uuid"})
(defmethod convert-class java.util.Date          [_ _] {:type "string" :format "date-time"})
(defmethod convert-class org.joda.time.DateTime  [_ _] {:type "string" :format "date-time"})
(defmethod convert-class org.joda.time.LocalDate [_ _] {:type "string" :format "date"})
(defmethod convert-class org.joda.time.LocalTime [_ _] {:type "string" :format "time"})
(defmethod convert-class java.util.regex.Pattern [_ _] {:type "string" :format "regex"})
(defmethod convert-class java.io.File            [_ _] {:type "file"})

(extension/java-time
  (defmethod convert-class java.time.Instant   [_ _] {:type "string" :format "date-time"})
  (defmethod convert-class java.time.LocalDate [_ _] {:type "string" :format "date"})
  (defmethod convert-class java.time.LocalTime [_ _] {:type "string" :format "time"}))

(defmethod convert-class :default [e _]
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
   (->swagger x {}))
  ([x options]
   (-> x
       (convert options)
       (merge-meta x options))))

(defn- try->swagger [v k key-meta opts]
  (try (->swagger v (-> opts
                        (assoc :key-meta key-meta)
                        (assoc :schema-type (opts->schema-type opts))))
       (catch Exception e
         (throw
           (IllegalArgumentException.
             (str "error converting to swagger schema [" k " "
                  (try (s/explain v) (catch Exception _ v)) "]") e)))))

(defn- coll-schema [e options]
  (-> {:type "array"
       :items (->swagger (first e) (assoc options ::no-meta true))}
      (assoc-collection-format options)))

(extend-protocol JsonSchema

  Object
  (convert [e _]
    (not-supported! e))

  Class
  (convert [e options]
    (if-let [schema (common/record-schema e)]
      (schema-object schema options)
      (convert-class e options)))

  nil
  (convert [_ _]
    nil)

  FieldSchema
  (convert [e options]
    (->swagger (:schema e) options))

  schema.core.Predicate
  (convert [e options]
    (some-> e :pred-name predicate-name-to-class (->swagger options)))

  schema.core.EnumSchema
  (convert [e options]
    (merge (->swagger (class (first (:vs e))) options) {:enum (seq (:vs e))}))

  schema.core.Maybe
  (convert [e {:keys [in] :as options}]
    (let [schema       (->swagger (:schema e) options)
          schema-type  (opts->schema-type options)
          nullable-key (if (= schema-type :openapi) :nullable :x-nullable)]
      (condp contains? in
        #{:query :formData} (assoc schema :allowEmptyValue true)
        #{nil :body} (assoc schema nullable-key true)
        schema)))

  schema.core.Both
  (convert [e options]
    (->swagger (first (:schemas e)) options))

  schema.core.Either
  (convert [e options]
    (->swagger (first (:schemas e)) options))

  schema.core.Recursive
  (convert [e options]
    (->swagger (:derefable e) options))

  schema.core.EqSchema
  (convert [e options]
    (merge (->swagger (class (:v e)) options)
           {:enum [(:v e)]}))

  schema.core.NamedSchema
  (convert [e options]
    (->swagger (:schema e) options))

  schema.core.One
  (convert [e options]
    (->swagger (:schema e) options))

  schema.core.AnythingSchema
  (convert [_ {:keys [in] :as opts}]
    (if (and in (not= :body in))
      (->swagger (s/maybe s/Str) opts)
      {}))

  schema.core.ConditionalSchema
  (convert [e options]
    (let [schema-type (opts->schema-type options)
          schema      (vec (keep #(->swagger (second %) options) (:preds-and-schemas e)))
          schema-key  (if (= schema-type :openapi) :oneOf :x-oneOf)]
      {schema-key schema}))

  schema.core.CondPre
  (convert [e options]
    (let [schema-type (opts->schema-type options)
          schema      (mapv #(->swagger % options) (:schemas e))
          schema-key  (if (= schema-type :openapi) :oneOf :x-oneOf)]
      {schema-key schema}))

  schema.core.Constrained
  (convert [e options]
    (->swagger (:schema e) options))

  java.util.regex.Pattern
  (convert [e _]
    {:type "string" :pattern (str e)})

  ;; Collections

  clojure.lang.Sequential
  (convert [e options]
    (coll-schema e options))

  clojure.lang.IPersistentSet
  (convert [e options]
    (assoc (coll-schema e options) :uniqueItems true))

  clojure.lang.IPersistentMap
  (convert [e {:keys [properties?] :as opts}]
    (if properties?
      {:properties (properties e opts)}
      (reference e opts)))

  clojure.lang.Var
  (convert [e opts]
    (reference e opts)))

;;
;; Schema to Swagger Schema definitions
;;

(defn properties
  "Take a map schema and turn them into json-schema properties.
  The result is put into collection of same type as input schema.
  Thus linked/map should keep the order of items. Returnes nil
  if no properties are found."
  ([schema] (properties schema nil))
  ([schema opts]
   {:pre [(common/plain-map? schema)]}
   (let [props (into (empty schema)
                     (for [[k v] schema
                           :when (s/specific-key? k)
                           :let [key-meta (meta k)
                                 k (s/explicit-schema-key k)]
                           :let [v (try->swagger v k key-meta opts)]]
                       (and v [k v])))]
     (if (seq props)
       props))))

(defn additional-properties
  "Generates json-schema additional properties from a plain map
  schema from under key s/Keyword."
  ([schema] (additional-properties schema nil))
  ([schema opts]
   {:pre [(common/plain-map? schema)]}
   (if-let [extra-key (s/find-extra-keys-schema schema)]
     (let [v (get schema extra-key)]
       (try->swagger v s/Keyword nil opts))
     false)))

(defn schema-object
  "Returns a JSON Schema object of a plain map schema."
  ([schema] (schema-object schema nil))
  ([schema opts]
   (if (common/plain-map? schema)
     (let [properties (properties schema opts)
           title (if (not (s/schema-name schema)) (common/title schema))
           additional-properties (additional-properties schema opts)
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
            :required required}))))))
