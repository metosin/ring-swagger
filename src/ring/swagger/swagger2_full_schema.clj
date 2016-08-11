(ns ring.swagger.swagger2-full-schema
  "Full swaggger spec Schema"
  (:require [schema.core :as s])
  (:import (java.net MalformedURLException)))

(defn opt [x] (s/optional-key x))

(def X- (s/pred #(re-matches #"x-.*" (name %)) ":x-.*"))

(defn length-greater [l]
  (s/pred
    (fn [x]
      (> (count x) l))))

(defn matches [r]
  (s/pred
    (fn [s]
      (re-matches r s))))

(def URL
  (s/constrained
    s/Str
    (fn [x]
      (try (java.net.URL. x)
           (catch MalformedURLException _ false)))
    "valid URL"))

(s/defschema ExternalDocs
  {:url URL
   (opt :description) s/Str})

(s/defschema Contact
  {(opt :name) s/Str
   (opt :url) URL
   (opt :email) (s/both (length-greater 5) (matches #".*@.*"))
   X- s/Any})

(s/defschema License
  {:name s/Str
   (opt :url) URL
   X- s/Any})

(s/defschema Info
  {:version s/Str
   :title s/Str
   (opt :description) s/Str
   (opt :termsOfService) s/Str
   (opt :contact) Contact
   (opt :license) License
   X- s/Any})

(s/defschema Tag
  {:name s/Str
   (opt :description) s/Str
   (opt :externalDocs) ExternalDocs
   X- s/Any})

(s/defschema Scheme
  (s/enum :http :https :ws :wss
          "http" "https" "ws" "wss"))

(s/defschema Schema
  {s/Any s/Any})                                            ;;TODO - http://swagger.io/specification/#schemaObject

(s/defschema Header
  {s/Any s/Any})                                            ;;TODO - http://swagger.io/specification/#headerObject

(s/defschema Headers
  {s/Any Header})

(s/defschema Example
  {s/Any s/Any})                                            ;;TODO - http://swagger.io/specification/#exampleObject

(s/defschema Response
  {:description s/Str
   (opt :schema) Schema
   (opt :headers) Headers
   (opt :examples) Example
   X- s/Any})

(s/defschema Responses
  s/Any                                                     ;;TODO - http://swagger.io/specification/#responsesObject
  #_{(s/either (s/eq :default) s/Int) (s/maybe {:s s/Str})
     #"x-" {:t s/Str}})

(s/defschema ResponsesDefinitions
  {s/Any Response})

(s/defschema Parameter
  (s/enum :query :header :path :formData :body))

(s/defschema ParametersDefinitions
  {Parameter s/Any})

(s/defschema SecurityRequirement
  {s/Keyword [s/Str]})

(s/defschema Operation
  {(opt :tags) [(s/either s/Str s/Keyword)]
   (opt :summary) s/Str
   (opt :description) s/Str
   (opt :externalDocs) ExternalDocs
   (opt :operationId) s/Str
   (opt :consumes) [s/Str]
   (opt :produces) [s/Str]
   (opt :parameters) ParametersDefinitions
   (opt :responses) Responses
   (opt :schemes) [Scheme]
   (opt :security) [SecurityRequirement]
   X- s/Any})

(s/defschema PathItem
  {(opt :get) Operation
   (opt :put) Operation
   (opt :post) Operation
   (opt :delete) Operation
   (opt :options) Operation
   (opt :head) Operation
   (opt :patch) Operation
   (opt :parameters) ParametersDefinitions})

(s/defschema Paths
  {(matches #"/.*") PathItem})                              ;;TODO - missing x-keys as schema can't do 2 different checks of key

(s/defschema SecurityDefinitions                            ;;TODO - http://swagger.io/specification/#securityDefinitionsObject
  {s/Keyword s/Any})

(s/defschema Swagger
  {(opt :swagger) (s/eq "2.0")                              ; has defaults
   (opt :info) Info                                         ; has defaults
   (opt :host) s/Str
   (opt :externalDocs) ExternalDocs
   (opt :basePath) s/Str
   (opt :consumes) [s/Str]
   (opt :produces) [s/Str]
   (opt :paths) Paths
   (opt :schemes) [Scheme]
   (opt :parameters) ParametersDefinitions
   (opt :responses) ResponsesDefinitions
   (opt :securityDefinitions) SecurityDefinitions
   (opt :security) [SecurityRequirement]
   (opt :tags) [Tag]
   X- s/Any})

