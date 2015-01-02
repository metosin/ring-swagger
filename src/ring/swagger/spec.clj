(ns ring.swagger.spec
  (require [schema.core :as s]
           [plumbing.core :refer [fn->>]]))

;;
;; helpers
;;

(defn regexp [r n] (s/pred (partial re-find r) n))
(defn valid-response-key? [x] (or (= :default x) (integer? x)))

;;
;; schemas
;;

(def VendorExtension {(s/pred (fn->> name (re-find #"^x-")) "vendor extension") s/Any})

(s/defschema ExternalDocs {:url s/Str
                           (s/optional-key :description) s/Str})

(s/defschema Info (merge
                    VendorExtension
                    {:version s/Str
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

(s/defschema Ref {:$ref s/Str})

(s/defschema Parameter
             (s/either
               (merge
                 VendorExtension
                 {:name s/Str
                  :in (s/enum :query, :header, :path, :formData)
                  (s/optional-key :description) s/Str
                  (s/optional-key :required) s/Bool
                  (s/optional-key :type) (s/enum "string" "number" "boolean" "integer" "array" "file")
                  (s/optional-key :format) s/Str
                  (s/optional-key :items) s/Any ; TODO: https://github.com/reverb/swagger-spec/blob/master/schemas/v2.0/schema.json#L401
                  (s/optional-key :collectionFormat) s/Str})
               (merge
                 VendorExtension
                 {:name s/Str
                  :in (s/enum :body)
                  (s/optional-key :description) s/Str
                  (s/optional-key :required) s/Bool
                  (s/optional-key :schema) (s/either
                                             Ref
                                             {:type (s/enum "array")
                                              (s/optional-key :uniqueItems) s/Bool
                                              :items Ref})})))

(s/defschema Response {:description s/Str
                       (s/optional-key :schema) Schema
                       (s/optional-key :headers) [SerializableType]
                       (s/optional-key :examples) Example})

(s/defschema Responses (merge
                         ;VendorExtension TODO: More than one non-optional/required key schemata
                         {(s/pred valid-response-key?) Response}))


(s/defschema Operation {(s/optional-key :tags) [Tag]
                        (s/optional-key :summary) s/Str
                        (s/optional-key :description) s/Str
                        (s/optional-key :externalDocs) ExternalDocs
                        (s/optional-key :operationId) s/Str
                        (s/optional-key :consumes) [s/Str]
                        (s/optional-key :produces) [s/Str]
                        (s/optional-key :parameters) [Parameter] ;TODO: (s/either Parameter Ref) -> https://github.com/reverb/swagger-spec/blob/master/schemas/v2.0/schema.json#L236
                        :responses Responses
                        ;(s/optional-key :schemes) [Scheme]
                        ;(s/optional-key :security) s/Any
                        })

(s/defschema PathItem {(s/optional-key :ref) s/Str
                       (s/optional-key :get) Operation
                       (s/optional-key :put) Operation
                       (s/optional-key :post) Operation
                       (s/optional-key :delete) Operation
                       (s/optional-key :options) Operation
                       (s/optional-key :head) Operation
                       (s/optional-key :patch) Operation
                       (s/optional-key :parameters) [Parameter]})

(s/defschema Paths {(regexp #"^/.*[^\/]$" "valid path") PathItem})

(s/defschema Definitions {s/Keyword {s/Keyword s/Any}}) ; TODO: validate for real?

#_(s/defschema Parameters s/Any)
#_(s/defschema Security s/Any)

; TODO: Authorizations: https://github.com/metosin/ring-swagger/commit/0525294dc87c0f179244c61504ed990460041349
(s/defschema Swagger {:swagger (s/enum 2.0)
                      :info Info
                      ;(s/optional-key :externalDocs) ExternalDocs
                      ;(s/optional-key :host) s/Str
                      (s/optional-key :basePath) s/Str
                      ;(s/optional-key :schemes) [Scheme]
                      (s/optional-key :consumes) [s/Str]
                      (s/optional-key :produces) [s/Str]
                      :paths Paths
                      (s/optional-key :definitions) Definitions
                      ;(s/optional-key :parameters) Parameters
                      ;(s/optional-key :responses) Responses
                      ;(s/optional-key :security) Security
                      ;(s/optional-key :tags) [Tag]
                      })
