(ns ring.swagger.core
  (:require [clojure.string :as str]
            [ring.util.response :refer :all]
            [schema.core :as s]
            [clout.core :as clout]
            [ring.swagger.schema :as schema]
            [ring.swagger.common :refer :all]
            [cheshire.generate :refer [add-encoder]]
            [camel-snake-kebab :refer [->camelCase]]))

;;
;; Models
;;

(defrecord Route [method uri metadata])

;;
;; Schema Transformations
;;

(add-encoder clojure.lang.Var
  (fn [x jsonGenerator] (.writeString jsonGenerator (name-of x))))

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
      [k (merge (dissoc (meta v) :model) (type-of v))])))

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

;;
;; Route generation
;;

(defn extract-path-parameters [path]
  (-> path clout/route-compile :keys))

(defn swagger-path [path]
  (str/replace path #":([^/]+)" "{$1}"))

(defn generate-nick [{:keys [method uri]}]
  (-> (str (name method) " " uri)
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
      {:apis (map
               (fn [[api details]]
                 {:path (str "/" (name api))
                  :description (or (:description details) "")})
               swagger)
       :info (select-keys parameters api-declaration-keys)})))

(defn api-declaration [parameters basepath details]
  (response
    (merge
      swagger-defaults
      (select-keys parameters [:apiVersion])
      {:basePath basepath
       :resourcePath ""
       :produces ["application/json"]
       :models (apply transform-models (:models details))
       :apis (map
               (fn [{:keys [method uri metadata] :as route}]
                 (let [{:keys [return summary notes nickname parameters]} metadata]
                   {:path (swagger-path uri)
                    :operations [(merge
                                   (if return (return-type-of return) {:type "void"})
                                   {:method (-> method name .toUpperCase)
                                    :summary (or summary "")
                                    :notes (or notes "")
                                    :nickname (or nickname (generate-nick route))
                                    :parameters (concat
                                                  parameters
                                                  (map
                                                    (fn [path-parameter]
                                                      {:name (name path-parameter)
                                                       :description ""
                                                       :required true
                                                       :type "string"
                                                       :paramType "path"})
                                                    (extract-path-parameters uri)))})]}))
               (:routes details))})))
