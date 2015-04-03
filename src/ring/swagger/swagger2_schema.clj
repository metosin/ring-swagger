(ns ring.swagger.swagger2-schema
  "Schemas that Ring-Swagger expects from it's clients"
  (require [schema.core :as s]))

(defn opt [x] (s/optional-key x))
(def X- (s/pred #(re-find #"x-" (name %)) ":x-.*"))

(s/defschema ExternalDocs
  {:url s/Str
   (opt :description) s/Str})

(s/defschema Info
  {X- s/Any
   :version s/Str
   :title s/Str
   (opt :description) s/Str
   (opt :termsOfService) s/Str
   (opt :contact) {(opt :name) s/Str
                   (opt :url) s/Str
                   (opt :email) s/Str}
   (opt :license) {:name s/Str
                   (opt :url) s/Str}})

(s/defschema Tag (s/either s/Str s/Keyword))

(s/defschema Scheme (s/enum :http :https :ws :wss))

#_(s/defschema SerializableType {(opt :type) (s/enum :string :number :boolean :integer :array :file)
                               (opt :format) s/Str
                               (opt :items) s/Any
                               (opt :collectionFormat) s/Str})

#_(s/defschema Response
  {(opt :description) s/Str
   (opt :schema) s/Any                                      ; anything
   (opt :headers) s/Any                                     ; anything
   (opt :examples) s/Any})                                  ; anything

(s/defschema Responses
  s/Any                                                     ; anything
  #_{(s/either (s/eq :default) s/Int) (s/maybe {:s s/Str})
   #"x-" {:t s/Str}})

(s/defschema Parameters
  {(opt :body) s/Any
   (opt :query) s/Any
   (opt :path) s/Any
   (opt :header) s/Any
   (opt :formData) s/Any})

(s/defschema Operation
  {(opt :tags) [Tag]
   (opt :summary) s/Str
   (opt :description) s/Str
   (opt :externalDocs) ExternalDocs
   (opt :operationId) s/Str
   (opt :consumes) [s/Str]
   (opt :produces) [s/Str]
   (opt :parameters) Parameters
   (opt :responses) Responses
   ;(opt :schemes) [Scheme]
   ;(opt :security) s/Any
   s/Keyword s/Any})

(s/defschema Swagger
  {(opt :swagger) (s/eq "2.0")                              ; has defaults
   (opt :info) Info                                         ; has defaults
   (opt :externalDocs) ExternalDocs
   (opt :basePath) s/Str
   (opt :consumes) [s/Str]
   (opt :produces) [s/Str]
   (opt :paths) {s/Str {s/Keyword (s/maybe Operation)}}     ; has defaults
   (opt :schemes) [Scheme]
   ;(opt :externalDocs) ExternalDocs
   ;(opt :host) s/Str
   ;(opt :parameters) s/Any
   ;(opt :responses) Responses
   ;(opt :security) s/Any
   ;(opt :tags) [Tag]
   s/Keyword s/Any})
