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

(defrecord Route [method uri])

;;
;; Schema Transformations
;;

(add-encoder clojure.lang.Var
  (fn [x jsonGenerator] (.writeString jsonGenerator (name-of x))))

(defmulti json-type  identity)
(defmethod json-type s/Int    [_] {:type "integer" :format "int64"})
(defmethod json-type s/String [_] {:type "string"})
(defmethod json-type schema/sString  [_] {:type "string"})
(defmethod json-type :default [e]
  (cond
    (= (class e)
      schema.core.EnumSchema) {:type "string"
                               :enum (seq (:vs e))}
    (map? e) {:$ref (-> e meta :model name-of)}
    :else (throw (IllegalArgumentException. (str e)))))

(defn type-of [v]
  (if (sequential? v)
    {:type "array"
     :items (json-type (first v))}
    (json-type v)))

(defn properties [schema]
  (into {}
    (for [[k v] schema
          :let [k (s/explicit-schema-key k)]]
      [k (merge (dissoc (meta v) :model) (type-of v))])))

(defn required-keys [schema]
  (filter s/required-key? (keys schema)))

(defn purge-model-var [x]
  (cond
    (map? x)    (or (-> x meta :model) x)
    (symbol? x) (-> x eval recur)
    :else       (let [x' (eval x)]
                  (if (= (class x) (class x')) x (recur x')))))

(defn purge-model-vars [m]
  (into {} (for [[k v] m] [k (purge-model-var v)])))

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
    set
    (map transform)
    (map (juxt (comp keyword :id) identity))
    (into {})))

;;
;; Route generation
;;

(defn extract-basepath
  [{:keys [scheme server-name server-port]}]
  (str (name scheme) "://" server-name ":" server-port))

(defn extract-path-parameters [path]
  (-> path clout/route-compile :keys))

(defn swagger-path [path]
  (str/replace path #":([^/]+)" "{$1}"))

(defn generate-nick [{:keys [method uri]}]
  (-> (str (name method) " " uri)
    (str/replace #"/" " ")
    (str/replace #":" " by ")
    ->camelCase))

(def top-level-keys [:apiVersion])
(def info-keys      [:title :description :termsOfServiceUrl :contact :license :licenseUrl])

;;
;; Public api
;;

(defn api-listing [parameters swagger]
  (response
    (merge
      {:apiVersion "1.0.0"
       :swaggerVersion "1.2"
       :apis (map
               (fn [[api details]]
                 {:path (str "/" (name api))
                  :description (:description details)})
               swagger)
       :info (select-keys parameters info-keys)}
      (select-keys parameters top-level-keys))))

(defn api-declaration [details request]
  (response
    {:apiVersion "1.0.0"
     :swaggerVersion "1.2"
     :basePath (extract-basepath request)
     :resourcePath "" ;; TODO: should be supported?
     :produces ["application/json"]
     :models (apply transform-models (:models details))
     :apis (map
             (fn [[{:keys [method uri] :as route} {:keys [return summary notes nickname parameters]}]]
               {:path (swagger-path uri)
                :operations
                [{:method (-> method name .toUpperCase)
                  :summary (or summary "")
                  :notes (or notes "")
                  :type (or (schema/schema-name return) "json")
                  :nickname (or nickname (generate-nick route))
                  :parameters (into
                                parameters
                                (map
                                  (fn [path-parameter]
                                    {:name (name path-parameter)
                                     :description ""
                                     :required true
                                     :type "string"
                                     :paramType "path"})
                                  (extract-path-parameters uri)))}]})
             (:routes details))}))
