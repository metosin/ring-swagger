(ns ring.swagger.swagger2-schema
  (require [schema.core :as s]
           [plumbing.core :refer [fn->>]]))

;;
;; helpers
;;

(defn regexp [r n] (s/pred (fn->> name (re-find r)) n))
(defn valid-response-key? [x] (or (= :default x) (integer? x)))

;;
;; schemas
;;

(def vendor-extension (regexp #"x-" 'vendor-extension))

(s/defschema ExternalDocs {:url s/Str
                           (s/optional-key :description) s/Str})

(s/defschema Info (merge
                    {vendor-extension s/Any
                     :version s/Str
                     :title   s/Str
                     (s/optional-key :description) s/Str
                     (s/optional-key :termsOfService) s/Str
                     (s/optional-key :contact) {(s/optional-key :name) s/Str
                                                (s/optional-key :url) s/Str
                                                (s/optional-key :email) s/Str}
                     (s/optional-key :licence) {:name s/Str
                                                (s/optional-key :url) s/Str}}))

(s/defschema Tag (s/either s/Str s/Keyword))

; TODO
(s/defschema Schema s/Any)

(s/defschema Scheme (s/enum :http :https :ws :wss))

(s/defschema SerializableType {(s/optional-key :type) (s/enum :string :number :boolean :integer :array :file)
                               (s/optional-key :format) s/Str
                               (s/optional-key :items) s/Any
                               (s/optional-key :collectionFormat) s/Str})

; TODO
(s/defschema Example s/Any)

; TODO
(s/defschema Schema s/Any)

(s/defschema Response {:description s/Str
                       (s/optional-key :schema) Schema
                       (s/optional-key :headers) [SerializableType]
                       (s/optional-key :examples) Example})

(s/defschema Responses (merge
                         ;; TODO: More than one non-optional/required key schemata
                         ;vendor-extension s/Any
                         {(s/pred valid-response-key?) Response}))

(s/defschema Parameters {(s/optional-key :body) s/Any
                         (s/optional-key :query) s/Any
                         (s/optional-key :path) s/Any
                         (s/optional-key :header) s/Any
                         (s/optional-key :formData) s/Any})

(s/defschema Operation {(s/optional-key :tags) [Tag]
                        (s/optional-key :summary) s/Str
                        (s/optional-key :description) s/Str
                        (s/optional-key :externalDocs) ExternalDocs
                        (s/optional-key :operationId) s/Str
                        (s/optional-key :consumes) [s/Str]
                        (s/optional-key :produces) [s/Str]
                        (s/optional-key :parameters) Parameters
                        :responses Responses
                        ;(s/optional-key :schemes) [Scheme]
                        ;(s/optional-key :security) s/Any
                        })

#_(s/defschema Parameters s/Any)
#_(s/defschema Security s/Any)

; TODO: Authorizations: https://github.com/metosin/ring-swagger/commit/0525294dc87c0f179244c61504ed990460041349
(s/defschema Swagger {(s/optional-key :swagger) (s/enum "2.0")
                      (s/optional-key :info) Info
                      ;(s/optional-key :externalDocs) ExternalDocs
                      ;(s/optional-key :host) s/Str
                      (s/optional-key :basePath) s/Str
                      ;(s/optional-key :schemes) [Scheme]
                      (s/optional-key :consumes) [s/Str]
                      (s/optional-key :produces) [s/Str]
                      :paths {s/Str {s/Keyword Operation}}
                      ;(s/optional-key :parameters) Parameters
                      ;(s/optional-key :responses) Responses
                      ;(s/optional-key :security) Security
                      ;(s/optional-key :tags) [Tag]
                      })
