(ns ring.swagger.core
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [ring.util.response :refer :all]
            [schema.core :as s]
            [schema.utils :as su]
            [ring.swagger.data :as data]
            [ring.swagger.schema :as schema]
            [ring.swagger.coerce :as coerce]
            [ring.swagger.common :refer :all]
            [cheshire.generate :refer [add-encoder]]
            [camel-snake-kebab :refer [->camelCase]]))

;;
;; Models
;;

(s/defschema Route {:method   s/Keyword
                    :uri      [s/Any]
                    :metadata {s/Keyword s/Any}})

;;
;; JSON Encoding
;;

(add-encoder clojure.lang.Var
  (fn [x jsonGenerator]
    (.writeString jsonGenerator (name-of x))))

(add-encoder schema.utils.ValidationError
  (fn [x jsonGenerator]
    (.writeString jsonGenerator
      (str (su/validation-error-explain x)))))

(defn date-time-encoder [x jsonGenerator]
  (.writeString jsonGenerator (coerce/unparse-date-time x)))

(add-encoder java.util.Date date-time-encoder)
(add-encoder org.joda.time.DateTime date-time-encoder)

(add-encoder org.joda.time.LocalDate
  (fn [x jsonGenerator]
    (.writeString jsonGenerator (coerce/unparse-date x))))

;;
;; Schema Transformations
;;

(defn resolve-model-var [x]
  (cond
    (map? x)    (or (-> x meta :model) x)
    (symbol? x) (-> x eval recur)
    :else       (let [x' (eval x)]
                  (if (= (class x) (class x')) x (recur x')))))

;;
;; Json Schema transformations
;;

(declare json-type)

(defn ->json
  [x & {:keys [top] :or {top false}}]
  (letfn [(type-of [x] (json-type (or (schema/type-map x) x)))]
    (cond
      (nil? x)        {:type "void"}
      (sequential? x) {:type "array"
                       :items (type-of (first x))}
      (set? x)        {:type "array"
                       :uniqueItems true
                       :items (type-of (first x))}
      :else           (if top
                        {:type (schema/model-name x)}
                        (type-of x)))))
;;
;; dispatch
;;

(defmulti json-type identity)
(defmulti json-type-class (fn [e] (class e)))

;;
;; identity-based dispatch
;;

(defmethod json-type data/Long*     [_] {:type "integer" :format "int64"})
(defmethod json-type data/Double*   [_] {:type "number" :format "double"})
(defmethod json-type data/String*   [_] {:type "string"})
(defmethod json-type data/Boolean*  [_] {:type "boolean"})
(defmethod json-type data/Keyword*  [_] {:type "string"})
(defmethod json-type data/DateTime* [_] {:type "string" :format "date-time"})
(defmethod json-type data/Date*     [_] {:type "string" :format "date"})

(defmethod json-type :default [e]
  (or
    (json-type-class e)
    (cond
      (schema/model? e) {:$ref (schema/model-name e)}
      (schema/model? (value-of (resolve-model-var e))) {:$ref (schema/model-name e)}
      :else (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e))))))

;;
;; class-based dispatch
;;

(defmethod json-type-class schema.core.EnumSchema [e] (merge (->json (class (first (:vs e)))) {:enum (seq (:vs e))}))
(defmethod json-type-class schema.core.Maybe      [e] (->json (:schema e)))
(defmethod json-type-class schema.core.Both       [e] (->json (first (:schemas e))))
(defmethod json-type-class schema.core.Recursive  [e] (->json (:schema-var e)))
(defmethod json-type-class schema.core.EqSchema   [e] (->json (class (:v e))))
(defmethod json-type-class :default [e])

;;
;; Common
;;

(defn properties [schema]
  (into {}
    (for [[k v] schema
          :let [k (s/explicit-schema-key k)]]
      [k (merge
           (dissoc (meta v) :model :name)
           (try (->json v)
             (catch Exception e
               (throw
                 (IllegalArgumentException.
                   (str "error converting to json schema [" k " " (s/explain v) "]") e)))))])))

(defn required-keys [schema]
  (filter s/required-key? (keys schema)))

(defn resolve-model-vars [form]
  (walk/prewalk (fn [x] (or (schema/model-var x) x)) form))

;;
;; public Api
;;

(defn transform [schema*]
  (let [schema (value-of schema*)
        required (required-keys schema)
        required (if-not (empty? required) required)]
    (remove-empty-keys
      {:id (name-of schema*)
       :properties (properties schema)
       :required required})))

(defn collect-models [x]
  (set
    (let [value (value-of x)
          model  (schema/model-var value)
          values (cond
                   (map? value) (vals value)
                   (sequential? value) value
                   :else [])
          cols   (filter coll? values)
          models (->> cols (map meta) (keep :model))
          models (if model (conj models model) model)]
      (reduce concat models (map collect-models cols)))))

(defn transform-models [& schemas*]
  (->> schemas*
    (mapcat collect-models)
    (map transform)
    (map (juxt (comp keyword :id) identity))
    (into {})))

(defn extract-models [details]
  (let [route-meta (->> details :routes (map :metadata))
        return-models (->> route-meta (keep :return) flatten)
        parameter-models (->> route-meta (mapcat :parameters) (keep :type) flatten)]
    (-> return-models
      (into parameter-models)
      flatten
      distinct
      vec)))

;;
;; Route generation
;;

(defn path-params [s]
  (-> s
    (str/replace #":(.[^:|/]*)" " :$1 ")
    (str/split #" ")
    (->> (keep #(if (.startsWith % ":") (keyword (.substring % 1)))))))

; move this to client, use Schame types
(defn swagger-path-parameters [uri]
  (for [p (path-params uri)]
    {:name (name p)
     :description ""
     :required true
     :type "string"
     :paramType "path"}))

(defn swagger-path [uri]
  (str/replace uri #":([^/]+)" "{$1}"))

(defn generate-nick [{:keys [method uri]}]
  (-> (str (name method) " " uri)
    (str/replace #"/" " ")
    (str/replace #"-" "_")
    (str/replace #":" " by ")
    ->camelCase))

(def swagger-defaults      {:swaggerVersion "1.2" :apiVersion "0.0.1"})
(def api-declaration-keys  [:title :description :termsOfServiceUrl :contact :license :licenseUrl])

(defn extract-basepath
  [{:keys [scheme server-name server-port]}]
  (str (name scheme) "://" server-name ":" server-port))

(defn convert-parameter [{:keys [type] :as parameter}]
  (if (string? type) ;; already typed to JSON Schema
      parameter
      (merge
        parameter
        (->json type))))

;;
;; Public api
;;

(defn api-listing [parameters swagger]
  (response
    (merge
      swagger-defaults
      (select-keys parameters [:apiVersion])
      {:info (select-keys parameters api-declaration-keys)
       :apis (for [[api details] swagger]
               {:path (str "/" (name api))
                :description (or (:description details) "")})})))

(defn api-declaration [parameters swagger api basepath]
  (if-let [details (and swagger (swagger api))]
    (response
      (merge
        swagger-defaults
        (select-keys parameters [:apiVersion])
        {:basePath basepath
         :resourcePath ""
         :produces ["application/json"]
         :models (apply transform-models (extract-models details))
         :apis (for [{:keys [method uri metadata] :as route} (:routes details)
                     :let [{:keys [return summary notes nickname parameters]} metadata]]
                 {:path (swagger-path uri)
                  :operations [(merge
                                 (->json return :top true)
                                 {:method (-> method name .toUpperCase)
                                  :summary (or summary "")
                                  :notes (or notes "")
                                  :nickname (or nickname (generate-nick route))
                                  :responseMessages [] ;; TODO
                                  :parameters (concat
                                                (map convert-parameter parameters)
                                                (swagger-path-parameters uri))})]})}))))
