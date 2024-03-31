(ns ring.openapi.openapi3
  (:require [clojure.string :as str]
            [schema.core :as s]
            [schema-tools.core :as stc]
            [plumbing.core :as p]
            [ring.swagger.common :as common]
            [ring.swagger.json-schema :as rsjs]
            [ring.swagger.core :as rsc]
            [ring.openapi.openapi3-schema :as openapi3-schema]))

;;
;; Schema transformations
;;

(defn extract-models [swagger]
  (let [route-meta (->> swagger
                        :paths
                        vals
                        (map vals)
                        flatten)
        body-models (->> route-meta
                         (map (comp :requestBody)))
        response-models (->> route-meta
                             (map :responses)
                             (mapcat vals)
                             (map :content)
                             (map vals))]
    [body-models response-models]))

(defn transform-models [schemas options]
  (->> schemas
       rsc/collect-models
       (rsc/handle-duplicate-schemas (:handle-duplicate-schemas-fn options))
       (map (juxt (comp str key) (comp #(rsjs/schema-object % :openapi) val)))
       (into (sorted-map))))

(defn extract-parameter [in model options]
  (if model
    (for [[k v] (-> model common/value-of stc/schema-value rsc/strict-schema)
          :when (s/specific-key? k)
          :let [rk (s/explicit-schema-key k)
                json-schema (rsjs/->swagger v options :openapi)]
          :when json-schema]
      {:in          (name in)
       :name        (rsjs/key-name rk)
       :description ""
       :required    (or (= in :path) (s/required-key? k))
       :schema      json-schema})))

(defn- default-response-description
  "uses option :default-response-description-fn to generate
   a default response description for status code"
  [status options]
  (if-let [generator (:default-response-description-fn options)]
    (generator status)
    ""))

(defn convert-content-schema [contents options]
  (if contents
    (into {} (for [[content-type schema-input] contents]
               [content-type
                (let [schema      (rsc/peek-schema schema-input)
                      schema-json (rsjs/->swagger schema-input options :openapi)]
                  {:name   (or (common/title schema) "")
                   :schema schema-json})]))))


(defn convert-parameters [parameters options]
  (into [] (mapcat (fn [[in model]]
                     (extract-parameter in model (assoc options :in in)))
                   parameters)))

(defn convert-responses [responses options]
  (let [responses (p/for-map [[k v] responses
                              :let [{:keys [content headers]} v]]
                    k (-> v
                          (cond-> content (assoc :content (convert-content-schema content options)))
                          (cond-> headers (update-in [:headers] (fn [headers]
                                                                  (if headers
                                                                    (->> (for [[k v] headers]
                                                                           [k (rsjs/->swagger v options :openapi)])
                                                                         (into {}))))))
                          (update-in [:description] #(or %
                                                         (:description (rsjs/json-schema-meta v))
                                                         (:description v)
                                                         (default-response-description k options)))
                          common/remove-empty-keys))]
    (if-not (empty? responses)
      responses
      {:default {:description ""}})))

(defn convert-operation
  "Returns a map with methods as keys and the Operation
   maps with parameters and responses transformed to comply
   with Swagger spec as values"
  [operation options]
  (p/for-map [[k v] operation]
    k (-> v
          (common/update-in-or-remove-key [:parameters] #(convert-parameters % options) empty?)
          (common/update-in-or-remove-key [:requestBody :content] #(convert-content-schema % options) empty?)
          (update-in [:responses] convert-responses options))))

(defn swagger-path
  "Replaces Compojure/Clout style path params in uri with Swagger style
  path params.

  Does not support wildcard-paths or inline-regexes.

  The regex is copied from Clout."
  [uri]
  ;; TODO: In 1.0, leave it to client libs to build swagger style path template
  ;; Currently everyone needs to build Clout path is just extra step for all but
  ;; compojure-api.
  (str/replace uri #":([\p{L}_][\p{L}_0-9-]*)" "{$1}"))

(defn extract-paths-and-definitions [swagger options]
  (let [original-paths       (or (:paths swagger) {})
        paths                (reduce-kv
                              (fn [acc k v]
                                (assoc acc
                                  (swagger-path k)
                                  (convert-operation v options)))
                              (empty original-paths)
                              original-paths)
        definitions (-> swagger
                        extract-models
                        (transform-models options))]
    [paths definitions]))

(defn process-contents [content prefix]
  (into {} (for [[content-type schema] content]
             [content-type (rsc/with-named-sub-schemas schema prefix)])))

(defn ensure-body-sub-schemas [route]
  (update-in route [:requestBody :content]
             #(process-contents % "Body")))

(defn ensure-response-sub-schemas [route]
  (if-let [responses (get-in route [:responses])]
    (let [schema-codes (reduce (fn [acc [k {:keys [content]}]]
                                 (if content (conj acc k) acc))
                               [] responses)
          transformed (reduce (fn [acc code]
                                (update-in acc [:responses code :content] #(process-contents % "Response")))
                              route schema-codes)]
      transformed)
    route))

(defn get-response-ref [v]
  (some-> (-> v
              :content
              vals
              first
              :schema
              :$ref)
          (str/replace "/schemas/" "/responses/")))

(defn to-responses-defn [responses]
  (into {} (for [[method status-ref-map] responses] [method  (into {} (for [[status [references]] status-ref-map] [status {:$ref references}]))])))

(defn endpoint-processor2 [endpoint]
  (let [backup          (reduce-kv (fn [acc method definition]
                            (let [body-acc      (if (:requestBody definition)
                                                  (let [body-name (-> (get-in definition [:requestBody :content])
                                                                      vals
                                                                      first
                                                                      :name)]
                                                    (-> acc
                                                        (update-in [:requestBodySchemas] conj {(keyword body-name) (:requestBody definition)})
                                                        (update-in [:requestBodyDefinitions method] conj (str "#/components/requestBodies/" body-name)))) acc)
                                  responses-acc (reduce-kv (fn [acc-res k v]
                                                             (let [response-path (get-response-ref v)
                                                                   response-name (last (.split response-path "/"))
                                                                   response-path-val (keyword response-name)]
                                                               (-> acc-res
                                                                   (update-in [:responses method k] conj response-path)
                                                                   (update-in [:responses-schema] conj {response-path-val v})))) body-acc (:responses definition))]
                              responses-acc))
                          {} endpoint)
        responses-map       (to-responses-defn (:responses backup))
        response-refs-updated     (reduce-kv (fn [acc http-method v]
                                     (assoc-in acc [http-method :responses] v)) endpoint responses-map)
        req-body-refs-updated (reduce-kv (fn [acc http-method [schema-reference]]
                                          (assoc-in acc [http-method :requestBody] {:$ref schema-reference})) response-refs-updated (:requestBodyDefinitions backup))]
    {:requestBodySchemas (:requestBodySchemas backup) :responses-schema (:responses-schema backup) :endpoint req-body-refs-updated}))

(defn remove-body-name [{:keys [content]}]
  {:content (into {} (for [[k v] content] [k (dissoc v :name)]))})

(defn move-schemas [swagger]
  (let [paths (or (:paths swagger) {})
        map-req-resp-schemas (for [[k v] paths] [k (endpoint-processor2 v)])
        updated-paths (into {} (for [[k v] map-req-resp-schemas] [k (:endpoint v)]))
        all-schemas (for [[_ v] map-req-resp-schemas] [(dissoc v :endpoint)])
        request-bodies  (into {} (flatten (mapv (fn [x] (map :requestBodySchemas x)) (vec all-schemas))))
        request-bodies  (into {} (for [[body-name schema] request-bodies] [body-name (remove-body-name schema)]))
        responses-schema (into {} (flatten (map (fn [x] (map :responses-schema x)) (vec all-schemas))))
        swagger-new       (-> swagger
                              (assoc :paths updated-paths)
                              (assoc-in [:components :responses] responses-schema)
                              (assoc-in [:components :requestBodies] request-bodies))]
    (clojure.pprint/pprint request-bodies)
    swagger-new))

;;
;; Public API
;;

;;
;; Transforming the spec
;;

(defn transform-operations
  "Transforms the operations under the :paths of a ring-swagger spec by applying (f operation)
  to all operations. If the function returns nil, the given operation is removed."
  [f swagger]
  (let [initial-paths (:paths swagger)
        transformed (for [[path endpoints] initial-paths
                          [method endpoint] endpoints
                          :let [endpoint (f endpoint)]]
                      [[path method] endpoint])
        paths (reduce (fn [acc [kv endpoint]]
                        (if endpoint
                          (assoc-in acc kv endpoint)
                          acc)) (empty initial-paths) transformed)]
    (assoc-in swagger [:paths] paths)))

(defn ensure-body-and-response-schema-names
  "Takes a ring-swagger spec and returns a new version
   with a generated names for all anonymous nested schemas
   that come as body parameters or response models."
  [swagger]
  (->> swagger
       (transform-operations ensure-body-sub-schemas)
       (transform-operations ensure-response-sub-schemas)))

;;
;; Schema
;;

(def openapi-defaults {:openapi  "3.0.3"
                       :info     {:title   "Swagger API"
                                  :version "0.0.1"}})
;;
;; Swagger Spec
;;

(defn security-processor [endpoint]
  (let [backup (reduce-kv (fn [acc method definition]
                            (if (:security definition)
                              (let [security         (:security definition)
                                    security-schemas (into {} (for [[k v] security] [k (dissoc v :scopes)]))
                                    security-path    (into {} (for [[k v] security] [k (:scopes v)]))
                                    result           (-> acc
                                                         (update-in [:security-paths method] conj security-path)
                                                         (update-in [:security-schemes] conj security-schemas))]
                                result) acc)) {} endpoint)
        new-endpoint  (reduce-kv (fn [acc http-method & security]
                                   (assoc-in acc [http-method :security] (vec (flatten security)))) endpoint (:security-paths backup))]
    {:security-schemes (:security-schemes backup) :endpoint new-endpoint}))

(defn security-operations [swagger]
  (let [paths (or (:paths swagger) {})
        map-req-resp-schemas (for [[k v] paths] [k (security-processor v)])
        updated-paths (into {} (for [[k v] map-req-resp-schemas] [k (:endpoint v)]))
        security-schemes (into {} (flatten (for [[_ v] map-req-resp-schemas] [(:security-schemes v)])))
        swagger-new       (-> swagger
                              (assoc :paths updated-paths)
                              (assoc-in  [:components :securitySchemes] security-schemes))]
    swagger-new))

(def OpenApi openapi3-schema/OpenApi)

(def Options {(s/optional-key :ignore-missing-mappings?) s/Bool
              (s/optional-key :default-response-description-fn) (s/=> s/Str s/Int)
              (s/optional-key :handle-duplicate-schemas-fn) s/Any})

(def option-defaults
  (s/validate Options {:ignore-missing-mappings? false
                       :default-response-description-fn (constantly "")
                       :handle-duplicate-schemas-fn rsc/ignore-duplicate-schemas}))

(s/defn openapi-json
  "Produces openapi-json output from ring-openapi spec.
   Optional second argument is a options map, supporting
   the following options with defaults:

   :ignore-missing-mappings?        - (false) boolean whether to silently ignore
                                      missing schema to JSON Schema mappings. if
                                      set to false, IllegalArgumentException is
                                      thrown if a Schema can't be presented as
                                      JSON Schema.

   :default-response-description-fn - ((constantly \"\")) - a fn to generate default
                                      response descriptions from http status code.
                                      Takes a status code (Int) and returns a String.

   :handle-duplicate-schemas-fn     - (ring.openapi.core/ignore-duplicate-schemas),
                                      a function to handle possible duplicate schema
                                      definitions. Takes schema-name and set of found
                                      attached schema values as parameters. Returns
                                      sequence of schema-name and selected schema value.

   :collection-format               - Sets the collectionFormat for query and formData
                                      parameters.
                                      Possible values: multi, ssv, csv, tsv, pipes."
  ([openapi :- (s/maybe OpenApi)] (openapi-json openapi nil))
  ([openapi :- (s/maybe OpenApi), options :- (s/maybe Options)]
   (let [options (merge option-defaults options)]
     (binding [rsjs/*ignore-missing-mappings* (true? (:ignore-missing-mappings? options))]
       (let [[paths definitions] (-> openapi
                                     ensure-body-and-response-schema-names
                                     (extract-paths-and-definitions options))]
         (common/deep-merge
          openapi-defaults
          (-> openapi
              (assoc :paths paths)
              (assoc-in [:components :schemas] definitions)
              (security-operations))))))))
