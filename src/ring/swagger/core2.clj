(ns ring.swagger.core2
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [ring.util.response :refer :all]
            [ring.swagger.impl :refer :all]
            [schema.core :as s]
            [plumbing.core :refer :all]
            [ring.swagger.common :refer :all]
            [ring.swagger.json-schema :as jsons]
            [ring.swagger.core :as core]))

(def Anything {s/Keyword s/Any})
(def Nothing {})

;;
;; defaults
;;

(def swagger-defaults {:swagger  "2.0"
                       :info     {:title "Swagger API"
                                  :version "0.0.1"}
                       :produces ["application/json"]
                       :consumes ["application/json"]})

;
;; Schema transformations
;;

(defn- requires-definition? [schema]
  (not (contains? #{nil Nothing Anything}
                  (s/schema-name schema))))

(defn extract-models [swagger]
  (let [route-meta      (->> swagger
                             :paths
                             vals
                             flatten)
        body-models     (->> route-meta
                             (map (comp :body :parameters))
                             (filter requires-definition?))
        response-models (->> route-meta
                             (map :responses)
                             (mapcat vals)
                             (map :schema)
                             flatten
                             (filter requires-definition?))
        all-models      (->> (concat body-models response-models)
                             flatten
                             (map core/with-named-sub-schemas))]
    (distinct all-models)))

(defn transform [schema]
  (let [required (required-keys schema)
        required (if-not (empty? required) required)]
    (remove-empty-keys
      {:properties (jsons/properties schema)
       :required required})))

(defn collect-models [x]
  (let [schemas (atom {})]
    (walk/prewalk
      (fn [x]
        (when (requires-definition? x)
          (swap! schemas assoc (s/schema-name x) (if (var? x) @x x)))
        x)
      x)
    @schemas))

(defn transform-models [schemas]
  (->> schemas
       (map collect-models)
       (apply merge)
       (map (juxt (comp keyword key) (comp transform val)))
       (into {})))

;;
;; Paths, parameters, responses
;;

(defmulti ^:private extract-parameter first)

;; TODO need to autogenerate names for schemas?
;; TODO specific format for :name ?
(defmethod extract-parameter :body [[type model]]
  (if-let [schema-name (s/schema-name model)]
    (vector {:in          type
             :name        (name schema-name)
             :description ""
             :required    true
             :schema      (str "#/definitions/" schema-name)})))

;; TODO jsons/->json should return :keyword types, or validate for strings?
(defmethod extract-parameter :default [[type model]]
  (if model
    (for [[k v] (-> model value-of strict-schema)
          :when (s/specific-key? k)
          :let [rk (s/explicit-schema-key (eval k))]]
      (jsons/->parameter {:in type
                          :name (name rk)
                          :required (s/required-key? k)}
                         (jsons/->json v)))))

(defn convert-parameters [parameters]
  (into [] (mapcat extract-parameter parameters)))

(defn convert-response-messages [responses]
  (letfn [(response-schema [schema]
            (if-let [name (s/schema-name schema)]
              (str "#/definitions/" name)
              (transform schema)))]
    (zipmap (keys responses)
            (map (fn [r] (update-in r [:schema] response-schema))
                 (vals responses)))))

(defn transform-path-operations
  "Returns a map with methods as keys and the Operation
   maps with parameters and responses transformed to comply
   with Swagger JSON spec as values"
  [operations]
  (into {} (map (juxt :method #(-> %
                                   (dissoc :method)
                                   (update-in [:parameters] convert-parameters)
                                   (update-in [:responses]  convert-response-messages)))
                operations)))

(defn swagger-path [uri]
  (str/replace uri #":([^/]+)" "{$1}"))

(defn extract-paths-and-definitions [swagger]
  (let [paths       (->> swagger
                         :paths
                         keys
                         (map swagger-path))
        methods     (->> swagger
                         :paths
                         vals
                         (map transform-path-operations))
        definitions (->> swagger
                         extract-models
                         transform-models)]
    (vector (zipmap paths methods) definitions)))

(defn swagger-json [swagger]
  (let [[paths definitions] (extract-paths-and-definitions swagger)]
    (merge
     swagger-defaults
     (-> swagger
          (assoc :paths paths)
          (assoc :definitions definitions)))))

;; https://github.com/swagger-api/swagger-spec/blob/master/schemas/v2.0/schema.json
;; https://github.com/swagger-api/swagger-spec/blob/master/examples/v2.0/json/petstore.json
;; https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md
