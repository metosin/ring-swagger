(ns ring.swagger.core
  (:require [clojure.string :as str]
            [ring.util.response :refer :all]
            [schema.core :as s]
            [schema.utils :as su]
            [ring.swagger.schema :as schema]
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
;; Schema Transformations
;;

(add-encoder clojure.lang.Var
  (fn [x jsonGenerator]
    (.writeString jsonGenerator (name-of x))))

(add-encoder schema.utils.ValidationError
  (fn [x jsonGenerator]
    (.writeString jsonGenerator
      (str (su/validation-error-explain x)))))


(defn resolve-model-var [x]
  (cond
    (map? x)    (or (-> x meta :model) x)
    (symbol? x) (-> x eval recur)
    :else       (let [x' (eval x)]
                  (if (= (class x) (class x')) x (recur x')))))

(defmulti json-type  identity)
(defmethod json-type s/Int [_] {:type "integer" :format "int64"})
(defmethod json-type s/Str [_] {:type "string"})
(defmethod json-type schema/Str* [_] {:type "string"})
(defmethod json-type :default [e]
  (cond
    (schema/enum? e)  {:type "string" :enum (seq (:vs e))}
    (schema/model? e) {:$ref (schema/schema-name e)}
    (schema/model? (value-of (resolve-model-var e))) {:$ref (schema/schema-name e)}
    :else (throw (IllegalArgumentException. (str "don't know how to create json-type of: " e)))))

(defn type-of [v]
  (if (sequential? v)
    {:type "array"
     :items (json-type (first v))}
    (json-type v)))

(defn return-type-of [v]
  (if (sequential? v)
    {:type "array"
     :items (json-type (first v))}
    {:type (schema/schema-name v)}))

(defn properties [schema]
  (into {}
    (for [[k v] schema
          :let [k (s/explicit-schema-key k)]]
      [k (merge
           (dissoc (meta v) :model)
           (try (type-of v)
             (catch Exception e
               (throw
                 (IllegalArgumentException.
                   (str "error converting to json schema [" k " " (s/explain v) "]") e)))))])))

(defn required-keys [schema]
  (filter s/required-key? (keys schema)))

;; walk it.
(defn resolve-model-vars [x]
  (cond
    (schema/model? x) (schema/model-of x)
    (map? x) (into {} (for [[k v] x] [k (resolve-model-var v)]))
    (sequential? x) (map resolve-model-var x)
    :else (resolve-model-var x)))

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
  (let [x      (value-of x)
        model  (-> x meta :model)
        values (if (map? x) (vals x) (seq x))
        cols   (filter coll? values)
        models (->> cols (map meta) (keep :model))
        models (if model (conj models model) model)]
    (reduce concat models (map collect-models cols))))

(defn transform-models [& schemas*]
  (->> schemas*
    (mapcat collect-models)
    distinct
    (map transform)
    (map (juxt (comp keyword :id) identity))
    (into {})))

(defn extract-models [details]
  (let [route-meta (->> details :routes (map :metadata))
        return-models (->> route-meta (keep :return) flatten)
        parameter-models (->> route-meta (mapcat :parameters) (keep :type))]
    (-> return-models
      (into parameter-models)
      flatten
      distinct
      vec)))

;;
;; Route generation
;;

(defn swagger-path-parameters [uri]
  (for [p (filter keyword? uri)]
    {:name (name p)
     :description ""
     :required true
     :type "string"
     :paramType "path"}))

(defn swagger-path [uri]
  (str/replace (str/join uri) #":([^/]+)" "{$1}"))

(defn generate-nick [{:keys [method uri]}]
  (-> (str (name method) " " (str/join uri))
    (str/replace #"/" " ")
    (str/replace #":" " by ")
    ->camelCase))

(def swagger-defaults      {:swaggerVersion "1.2" :apiVersion "0.0.1"})
(def api-declaration-keys  [:title :description :termsOfServiceUrl :contact :license :licenseUrl])

(defn extract-basepath
  [{:keys [scheme server-name server-port]}]
  (str (name scheme) "://" server-name ":" server-port))

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
                                 (if return (return-type-of return) {:type "void"})
                                 {:method (-> method name .toUpperCase)
                                  :summary (or summary "")
                                  :notes (or notes "")
                                  :nickname (or nickname (generate-nick route))
                                  :responseMessages [] ;; TODO
                                  :parameters (concat
                                                parameters
                                                (swagger-path-parameters uri))})]})}))))
