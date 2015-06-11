# Ring-Swagger [![Build Status](https://travis-ci.org/metosin/ring-swagger.svg?branch=master)](https://travis-ci.org/metosin/ring-swagger) [![Dependencies Status](http://jarkeeper.com/metosin/ring-swagger/status.png)](http://jarkeeper.com/metosin/ring-swagger)

[Swagger](http://swagger.io/) 1.2 & 2.0 implementation for Clojure/Ring using [Prismatic Schema](https://github.com/Prismatic/schema). 

- [API Docs](http://metosin.github.io/ring-swagger/doc/)
- Extendable trasformations from Prismatic Schemas to Swagger JSON Schemas
- Extended JSON & String Serialization & coercion
- For web/routing library developers:
  - A [Schema-based contract](https://github.com/metosin/ring-swagger/blob/master/src/ring/swagger/swagger2_schema.clj) for collecting route documentation from the web apps
  - Middlewares for handling Schemas Validation Errors
  - Handlers to generate the Swaggers artifacts
    - [swagger.json](https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#specification) for 2.0.
    - [Resource listing](https://github.com/swagger-api/swagger-spec/blob/master/versions/1.2.md#51-resource-listing) and [Api declarations](https://github.com/swagger-api/swagger-spec/blob/master/versions/1.2.md#52-api-declaration) for 1.2.
    - [Swagger-UI](https://github.com/swagger-api/swagger-ui) bindings. (get the UI separately as [jar](https://github.com/metosin/ring-swagger-ui) or from [NPM](https://www.npmjs.com/package/swagger-ui))

## Latest version

[![Clojars Project](http://clojars.org/metosin/ring-swagger/latest-version.svg)](http://clojars.org/metosin/ring-swagger)

## Web libs using Ring-Swagger

- [Compojure-Api](https://github.com/metosin/compojure-api) for Compojure
- [fnhouse-swagger](https://github.com/metosin/fnhouse-swagger) for fnhouse
- [pedastal-swagger](https://github.com/frankiesardo/pedestal-swagger) for Pedastal
- [yada](https://github.com/juxt/yada)

Route definitions as expected as a clojure Map defined by the [Schema](https://github.com/metosin/ring-swagger/blob/master/src/ring/swagger/swagger2_schema.clj). The Schema is open as ring-swagger tries not to be on your way - one can always pass any extra data in the Swagger Spec format.

### Simplest possible example

```clojure
(require '[ring.swagger.swagger2 :as rs])

(rs/swagger-json nil)

; {:swagger "2.0"
;  :info {:title "Swagger API"
;          :version "0.0.1"}
;  :produces ["application/json"]
;  :consumes ["application/json"]
;  :definitions {}
;  :paths {}}
```

### More complete example

... with info, tags, routes and anonymous nested schemas.

```clojure
(require '[schema.core :as s])

(s/defschema User {:id s/Str, 
                   :name s/Str
                   :address {:street s/Str
                             :city (s/enum :tre :hki)}})

(s/with-fn-validation 
  (rs/swagger-json 
    {:info {:version "1.0.0"
            :title "Sausages"
            :description "Sausage description"
            :termsOfService "http://helloreverb.com/terms/"
            :contact {:name "My API Team"
                      :email "foo@example.com"
                      :url "http://www.metosin.fi"}
            :license {:name "Eclipse Public License"
                      :url "http://www.eclipse.org/legal/epl-v10.html"}}
     :tags [{:name "user"
             :description "User stuff"}]
     :paths {"/api/ping" {:get nil}
             "/user/:id" {:post {:summary "User Api"
                                  :description "User Api description"
                                  :tags ["user"]
                                  :parameters {:path {:id s/Str}
                                               :body User}
                                  :responses {200 {:schema User
                                                   :description "Found it!"}
                                              404 {:description "Ohnoes."}}}}}}))

; {:swagger "2.0"
;  :info {:version "1.0.0"
;         :title "Sausages"
;         :description "Sausage description"
;         :termsOfService "http://helloreverb.com/terms/"
;         :contact {:email "foo@example.com"
;                   :name "My API Team"
;                   :url "http://www.metosin.fi"}
;         :license {:name "Eclipse Public License"
;                   :url "http://www.eclipse.org/legal/epl-v10.html"}}
;  :tags [{:description "User stuff" :name "user"}]
;  :consumes ["application/json"]
;  :produces ["application/json"]
;  :definitions {"User" {:type "object"
;                        :properties {:address {:$ref "#/definitions/UserAddress"}
;                                     :id {:type "string"}
;                                     :name {:type "string"}}
;                        :required (:id :name :address)}
;                "UserAddress" {:type "object"
;                               :properties {:city {:enum (:tre :hki)
;                                                   :type "string"}
;                                            :street {:type "string"}}
;                               :required (:street :city)}}
;   :paths {"/api/ping" {:get {:responses {:default {:description ""}}}}
;          "/user/{id}" {:post {:description "User Api description"
;                               :summary "User Api"
;                               :tags ["user"]
;                               :parameters [{:description ""
;                                             :in :path
;                                             :name "id"
;                                             :required true
;                                             :type "string"}
;                                            {:description ""
;                                             :in :body
;                                             :name "User"
;                                             :required true
;                                             :schema {:$ref "#/definitions/User"}}]
;                               :responses {200 {:description "Found it!"
;                                                :schema {:$ref "#/definitions/User"}}
;                                           404 {:description "Ohnoes."}}}}}}
 ```

## Customizing Swagger Spec output

One can pass extra options-map as a third parameter to `swagger-json`. The following options are available:

```clojure
 :ignore-missing-mappings?        - (false) boolean whether to silently ignore
                                    missing schema to JSON Schema mappings. if
                                    set to false, IllegalArgumentException is
                                    thrown if a Schema can't be presented as
                                    JSON Schema.

 :default-response-description-fn - ((constantly "")) - a fn to generate default
                                    response descriptions from http status code.
                                    Takes a status code (Int) and returns a String.

 :handle-duplicate-schemas-fn     - (ring.swagger.core/ignore-duplicate-schemas),
                                    a function to handle possible duplicate schema
                                    definitions. Takes schema-name and set of found
                                    attached schema values as parameters. Returns
                                    sequence of schema-name and selected schema value.
```

For example, to get default response descriptions from the [HTTP Spec](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes), you can do the following:

```clojure
(require '[ring.util.http-status :as status])

(rs/swagger-json
  {:paths {"/hello" {:post {:responses {200 nil
                                        425 nil
                                        500 {:description "FAIL"}}}}}}
  {:default-response-description-fn status/get-description})

; {:swagger "2.0"
;  :info {:title "Swagger API" :version "0.0.1"}
;  :consumes ["application/json"]
;  :produces ["application/json"]
;  :definitions {}
;  :paths {"/hello" {:post {:responses {200 {:description "OK"}
;                                       425 {:description "The collection is unordered."}
;                                       500 {:description "FAIL"}}}}}}
```

## Validating the Swagger Spec

The generated full spec can be validated against the [Swagger JSON Schema](https://raw.githubusercontent.com/reverb/swagger-spec/master/schemas/v2.0/schema.json) via tools like [scjsv](https://github.com/metosin/scjsv).

```clojure
(require '[scjsv.core :as scjsv])

(def validator (scjsv/validator (slurp "https://raw.githubusercontent.com/reverb/swagger-spec/master/schemas/v2.0/schema.json")))

(validator (rs/swagger-json {:paths {"/api/ping" {:get nil}}}))
; nil

(validator (rs/swagger-json {:pathz {"/api/ping" {:get nil}}}))
; ({:level "error"
;   :schema {:loadingURI "#", :pointer ""}
;   :instance {:pointer ""}
;   :domain "validation"
;   :keyword "additionalProperties"
;   :message "object instance has properties which are not allowed by the schema: [\"pathz\"]", :unwanted ["pathz"]})
```

For more information about creating your own adapter, see [Collecting API Documentation](https://github.com/metosin/ring-swagger/wiki/Collecting-API-Documentation).

## Web Schemas

[Prismatic Schema](https://github.com/Prismatic/schema) is used for modeling both the input & output schemas for routes.

As Swagger 2.0 Spec Schema is a pragmatic and deterministic subset of JSON Schema, so not all Clojure Schema elements can be used.

### Schema to Swagger JSON Schema transformation

There are two possible methods to do this:

1. class-based dispatch via `ring.swagger.json-schema/json-type`.
2. protocol-based dispatch via `ring.swagger.json-schema/JsonSchema`.

To support truly symmetric web schemas, one needs also to ensure both JSON Serialization and deserialization/coercion from JSON.

#### Class-based dispatch

```clojure
(require '[ring.swagger.json-schema :as json-schema])

(defmethod json-schema/json-type java.sql.Date [_] {:type "string" :format "date"})
```

#### Protocol-based dispatch

```clojure
(require '[ring.swagger.json-schema :as json-schema])

(extend-type java.util.regex.Pattern
  json-schema/JsonSchema
  (json-schema/json-property [e _] {:type "string" :pattern (str e)}))
```

### Out-of-the-box supported Schema elements

| Clojure Schema                              | JSON Schema              | Sample JSON |
| --------------------------------------------|--------------------------|:-----------:|
| `Integer`                                   | integer, int32           | `1` 
| `Long`, `s/Int`                             | integer, int64           | `1` 
| `Double`, `Number`, `s/Num`                 | number, double           | `1.2`
| `String`, `s/Str`, `Keyword`, `s/Keyword`   | string                   | `"kikka"`
| `Boolean`                                   | boolean                  | `true`
| `nil`, `s/Any`                              | void                     |
| `java.util.Date`, `org.joda.time.DateTime`  | string, date-time        | `"2014-02-18T18:25:37.456Z"`, also without millis: `"2014-02-18T18:25:37Z"`
| `java.util.regex.Pattern`,                  | string, regex            | `[a-z0-9]`
| `#"[a-z0-9]+"`                              | string, pattern          | `"a6"`
| `s/Uuid`, `java.util.UUID`                  | string, uuid             | `"77e70512-1337-dead-beef-0123456789ab"`
| `org.joda.time.LocalDate`                   | string, date             | `"2014-02-19"`
| `(s/enum X Y Z)`                            | *type of X*, enum(X,Y,Z)
| `(s/maybe X)`                               | *type of X*
| `(s/both X Y Z)`                            | *type of X*
| `(s/either X Y Z)`                          | *type of X*
| `(s/named X name)`                          | *type of X*
| `(s/one X name)`                            | *type of X*
| `(s/recursive Var)`                         | *Ref to (model) Var*
| `(s/eq X)`                                  | *type of class of X*
| `(s/optional-key X)`                        | *optional key*
| `(s/required-key X)`                        | *required key*
| `s/Keyword` (as a key)                      | *ignored*

- All supported types have symmetric JSON serialization (Cheshire encoders) & deserialization (Schema coercions)
- Vectors, Sets and Maps can be used as containers
- Maps are presented as Complex Types and References. Model references are resolved automatically.
  - Nested maps are transformed automatically into flat maps with generated child references
    - Nested maps can be within valid containers (as only element - heterogeneous schema sequences not supported by the spec)

### Missing Schema elements

If Ring-swagger can't transform the Schemas into JSON Schemas, by default a `IllegalArgumentException` will be thrown. Binding `ring.swagger.json-schema/*ignore-missing-mappings*` to true, one can ignore the errors (missing schema elements will be ignored from the generated JSON Schema).

### Body and Response model names

Standard Prismatic Schema names are used. Nested schemas are traversed and all found sub-schemas are named automatically (so that they can be referenced in the JSON Schema).

As Swagger 2.0 squases all endpoint models into a single namespace, name collisions can happen. When this happens, the `:handle-duplicate-schemas-fn` function is called to resolve the collision. By default, the collisions are ignored.

One common reason for the name collisions is to transform the schemas via normal `clojure.core` functions, which don't retain the original meta-data thus the schema name. 

```clojure
(s/defschema User {:id s/Str, :name s/Str})
(def NewUser (dissoc User :id)) ; dissoc does not touch the schema meta-data

(meta User)
; {:name Kikka}

(meta NewUser)
; {:name Kikka} <- fail!
```

There are better schema transformers functions available at [schema-tools](https://github.com/metosin/schema-tools).

### Extra Schema elements supported by `ring.swagger.json-schema-dirty`

Some Schema elements are impossible to accurately describe within boundaries of JSON-Schema or Swagger spec.
You can require `ring.swagger.json-schema-dirty` namespace to get JSON Schema dispatching for the following:

Be warned that Swagger-UI might not display these correctly and the code generated by swagger-codegen will be inaccurate.

| Clojure | JSON Schema | Sample  |
| --------|-------|:------------:|
| `(s/conditional pred X pred Y pred Z)` | oneOf: *type of X*, *type of X*, *type of Z*
| `(s/if pred X Y)` | oneOf: *type of X*, *type of Y*

### Currently Non-supported Schema elements

These schemas should work, just need the mappings (feel free to contribute!):

- `s/Symbol`
- `s/Inst`

### Schema coercion

Ring-swagger uses [Schema coercions](http://blog.getprismatic.com/blog/2014/1/4/schema-020-back-with-clojurescript-data-coercion) for transforming the input data into vanilla Clojure and back.

There are two coercers in `ring.swagger.coerce`, the `json-schema-coercion-matcher` and `query-schema-coercion-matcher`. These are enchanced versions of the orginal Schema coercers, adding support for all the supported Schema elements, including Dates & Regexps.

#### Coerce!

Ring-swagger provides a convenience function for coercion, `ring.swagger.schema/coerce!`. It returns either a valid coerced value of slingshots an Map with type `:ring.swagger.schema/validation`. One can catch these exceptions via `ring.swagger.middleware/wrap-validation-errors` and return a JSON-friendly map of the contents.

```clojure
(require '[schema.core :as s])
(require '[ring.swagger.schema :refer [coerce!]])

(s/defschema Bone {:size Long, :animal (s/enum :cow :tyrannosaurus)})

(coerce! Bone {:size 12, :animal "cow"})
; {:animal :cow, :size 12}

(coerce! Bone {:animal :sheep})
; ExceptionInfo throw+: #schema.utils.ErrorContainer{:error {:animal (not (#{:tyrannosaurus :cow} :sheep)), :size missing-required-key}, :type :ring.swagger.schema/validation}  ring.swagger.schema/coerce! (schema.clj:57)
```

## Adding description to Schemas

One can add extra meta-data, including descriptions to schema elements using `ring.swagger.schema/field` and `ring.swagger.schema/describe` functions. These work by adding meta-data to schema under `:json-schema`-key. Objects which don't support meta-data, like Java classes, are wrapped into `s/both`.

```clojure
(require '[schema.core :as s])
(require '[ring.swagger.schema :as rs])
(require '[ring.swagger.json-schema :as rjs])

(s/defschema Customer {:id Long, :name (rs/describe String "the name")})

(rjs/json-schema-meta (describe Customer "The Customer"))
; => {:description "The Customer"})
```

## License

Copyright © 2014-2015 [Metosin Oy](http://www.metosin.fi)

Distributed under the Eclipse Public License, the same as Clojure.
