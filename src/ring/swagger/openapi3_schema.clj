(ns ring.swagger.openapi3-schema
  (:require [schema.core :as s]
            [ring.swagger.swagger2-full-schema :refer [opt Info]]))

(s/defschema Server-Variable
  {(opt :enum) [s/Str]
   :default s/Str
   (opt :description) s/Str})

(s/defschema Server
  {:url s/Str
   (opt :description) s/Str
   (opt :variables) {s/Str Server-Variable}})

(s/defschema ExternalDocumentation
  {(opt :description) s/Str
   :url s/Str})

(s/defschema OpenApiSchemaPart
  {s/Keyword s/Any})

(s/defschema Example
  {(opt :summary) s/Str
   (opt :description) s/Str
   (opt :value) s/Any
   (opt :externalValue) s/Str})

(s/defschema Header
  {(opt :description) s/Str
   :required s/Bool
   (opt :deprecated) s/Bool
   (opt :allowEmptyValue) s/Bool
   (opt :style) s/Any
   (opt :explode) s/Bool
   (opt :schema) OpenApiSchemaPart
   (opt :example) s/Any
   (opt :examples) {s/Str Example}})

(s/defschema Encoding
  {(opt :contentType) s/Str
   (opt :headers) {s/Str Header}
   (opt :style) s/Any
   (opt :explode) s/Bool
   (opt :allowReserved) s/Bool})

(s/defschema MediaObject
  {(opt :schema) OpenApiSchemaPart
   (opt :example) Example
   (opt :examples) {s/Str Example}
   (opt :encoding) {s/Str Encoding}})

(s/defschema Parameter
  {(opt :query)  {s/Any s/Any}
   (opt :path)   {s/Any s/Any}
   (opt :header) {s/Any s/Any}})

(s/defschema RequestBody
  {(opt :description) s/Str
   :content           {s/Str MediaObject}
   (opt :required) s/Bool})

(s/defschema Link
  {(opt :operationRef) s/Str
   (opt :operationId) s/Str
   (opt :parameters) {s/Str s/Any}
   (opt :requestBody) s/Any
   (opt :description) s/Str
   (opt :server) Server})

(def ^:private codes [100 101 102 103
                      200 201 202 203 204 205 206 207 208 226
                      300 301 302 303 304 305 306 307 308
                      400 401 402 403 404 405 406 407 408 409 410 411 412 413 414 415 416 417 418 419 420 421 422 423 424 425 426 428 429 431 451
                      500 501 502 503 504 505 506 507 508 510 511])

(assert (apply distinct? codes))
(assert (apply < codes))

(s/defschema ResponseCode
  (apply s/enum (map str codes)))

(s/defschema Response
  {:description s/Str
   (opt :headers) {s/Str Header}
   (opt :content) {s/Str MediaObject}
   (opt :links) {s/Str Link}})

(s/defschema Operation
  {(opt :tags) [s/Str]
   (opt :summary) s/Str
   (opt :description) s/Str
   (opt :externalDocs) ExternalDocumentation
   (opt :operationId) s/Str
   (opt :parameters) Parameter
   (opt :requestBody) RequestBody
   (opt :responses) {ResponseCode Response}
   (opt :deprecated) s/Bool
   (opt :security) {s/Str [s/Str]}
   (opt :servers) [Server]})

(s/defschema Path
  {(opt :summary) s/Str
   (opt :description) s/Str
   (opt :get) Operation
   (opt :put) Operation
   (opt :post) Operation
   (opt :delete) Operation
   (opt :head) Operation
   (opt :patch) Operation
   (opt :servers) [Server]
   (opt :parameters) s/Any})

(s/defschema Callback
  {s/Str Path})

(s/defschema Tag
  {:name s/Str
   (opt :description) s/Str
   (opt :externalDocs) ExternalDocumentation})

(s/defschema SecuritySchemeApiKey
  {:type s/Any
   (opt :description) s/Str
   :name s/Str
   :in s/Any})

(s/defschema SecuritySchemeHttp
  {:type s/Any
   (opt :description) s/Str
   :scheme s/Str
   :bearerFormat s/Str})

(s/defschema SecurityScheme
  (s/conditional
    #(and (map? %) (= "apiKey" (:type %))) SecuritySchemeApiKey
    :else SecuritySchemeHttp))

(s/defschema Components
  {(opt :schemas) {s/Str OpenApiSchemaPart}
   (opt :responses) {s/Str Response}
   (opt :parameters) {s/Str Parameter}
   (opt :examples) {s/Str Example}
   (opt :requestBodies) {s/Keyword RequestBody}
   (opt :headers) {s/Str Header}
   (opt :securitySchemes) {s/Str SecurityScheme}
   (opt :links) {s/Str Link}
   (opt :callbacks) {s/Str Callback}})

(s/defschema OpenApi
  {(opt :openapi) (s/conditional string? (s/pred #(re-matches #"^3\.\d\.\d$" %)))
   (opt :info) Info
   (opt :servers) [Server]
   (opt :paths) {s/Str Path}
   (opt :components) Components
   (opt :security) {s/Str [s/Str]}
   (opt :tags) [Tag]
   (opt :externalDocs) ExternalDocumentation})
