# Ring-Swagger [![Build Status](https://travis-ci.org/metosin/ring-swagger.png?branch=master)](https://travis-ci.org/metosin/ring-swagger) [![Dependencies Status](http://jarkeeper.com/metosin/ring-swagger/status.png)](http://jarkeeper.com/metosin/ring-swagger)

[Swagger](http://swagger.io/) implementation for Clojure/Ring using [Prismatic Schema](https://github.com/Prismatic/schema) for data modeling. 

- Supports both 1.2 and 2.0 Swagger Specs
- For web developers
  - Extendable Schema-&gt;JSON Mappings with out-of-the-box support for most common types
  - Utilities for input & output Schema validation & coercion
- For web library developers:
  - A Schema-based contract for collecting route documentation from the web apps
  - Extendable Schema-&gt;JSON Schema conversion with out-of-the-box support for most Schema predicates
  - Common middleware for handling Schemas and Validation Errors.
  - Ring-handlers for exposing the swaggers artifacts
    - [swagger.json](https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#specification) for 2.0.
    - [Resource listing](https://github.com/swagger-api/swagger-spec/blob/master/versions/1.2.md#51-resource-listing) and [Api declarations](https://github.com/swagger-api/swagger-spec/blob/master/versions/1.2.md#52-api-declaration) for 1.2.
    - [Swagger-UI](https://github.com/swagger-api/swagger-ui) bindings. (the UI itself is jar-packaged [separately](https://github.com/metosin/ring-swagger-ui) or you can get it from [NPM](https://www.npmjs.com/package/swagger-ui))

## Latest version

[![Clojars Project](http://clojars.org/metosin/ring-swagger/latest-version.svg)](http://clojars.org/metosin/ring-swagger)

## Web libs using Ring-Swagger

- [Compojure-Api](https://github.com/metosin/compojure-api) for Compojure
- [fnhouse-swagger](https://github.com/metosin/fnhouse-swagger) for fnhouse
- [pedastal-swagger](https://github.com/frankiesardo/pedestal-swagger) for Pedastal

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
;  :produces ["application/json"]
;  :consumes ["application/json"]
;  :paths {"/api/ping" {:get {:responses {:default {:description ""}}}}
;          "/user/{id}" {:post {:parameters [{:type "string"
;                                             :in :path
;                                             :name "id"
;                                             :description ""
;                                             :required true}
;                                            {:in :body
;                                             :name "User"
;                                             :description ""
;                                             :required true
;                                             :schema {:$ref "#/definitions/User"}}]
;                               :summary "User Api"
;                               :description "User Api description"
;                               :responses {200 {:schema {:$ref "#/definitions/User"}
;                                                :description "User or not."}
;                                           404 {:description "Ohnoes."}}}}}
;  :definitions {"User" {:properties {:id {:type "string"}
;                                     :name {:type "string"}
;                                     :address {:$ref "#/definitions/UserAddress"}}
;                        :required [:id :name :address]}
;                "UserAddress" {:properties {:street {:type "string"}
;                                            :city {:enum (:tre :hki)
;                                                   :type "string"}}
;                               :required [:street :city]}}}
```

### validating the results

The generated full spec can be validated against the [Swagger JSON Schema](https://raw.githubusercontent.com/reverb/swagger-spec/master/schemas/v2.0/schema.json) via tools like [scjsv](https://github.com/metosin/scjsv).

```clojure
(require '[scjsv.core :as scjsv])

(def validator (scjsv/validator (slurp "https://raw.githubusercontent.com/reverb/swagger-spec/master/schemas/v2.0/schema.json")))

(validator (rs/swagger-json {:paths {"/api/ping" {:get nil}}}))
; nil

validator (rs/swagger-json {:pathz {"/api/ping" {:get nil}}}))
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

### Supported Schema elements

| Clojure Schema | JSON Schema | Sample JSON |
| --------|-------|:------------:|
| `Integer` | integer, int32 | `1` |
| `Long`, `s/Int` | integer, int64 | `1` |
| `Double`, `Number`, `s/Num`  | number, double | `1.2`
| `String`, `s/Str`, Keyword, `s/Keyword`      | string | `"kikka"`
| `Boolean`                   | boolean | `true`
| `nil`, `s/Any`              | void |
| `java.util.Date`, `org.joda.time.DateTime`  | string, date-time | `"2014-02-18T18:25:37.456Z"`, consumes also without millis: `"2014-02-18T18:25:37Z"`
| `java.util.regex.Pattern`,  | string, regex | `[a-z0-9]`
| `#"[a-z0-9]+"`              | string, pattern | `"a6"`
| `s/Uuid`, `java.util.UUID`  | string, uuid | `"77e70512-1337-dead-beef-0123456789ab"`
| `org.joda.time.LocalDate`   | string, date | `"2014-02-19"`
| `(s/enum X Y Z)`       | *type of X*, enum(X,Y,Z)
| `(s/maybe X)`          | *type of X*
| `(s/both X Y Z)`       | *type of X*
| `(s/either X Y Z)`     | *type of X*
| `(s/named X name)`     | *type of X*
| `(s/recursive Var)`    | *Ref to (model) Var*
| `(s/eq X)`    | *type of class of X*
| `(s/optional-key X)`    | *optional key*
| `(s/required-key X)`    | *required key*
| `s/Keyword` (as a key)  | *ignored*

- All supported types have symmetric JSON serialization (Cheshire encoders) & deserialization (Schema coercions)
- Vectors, Sets and Maps can be used as containers
  - Maps are presented as Complex Types and References. Model references are resolved automatically.
  - Nested maps are transformed automatically into flat maps with generated child references
    - Nested maps can be within valid containers (as only element - heterogeneous schema sequences not supported by the spec)

### Missing Schema elements

If ring-swagger can't transform the Schemas into JSON Schemas, by default a `IllegalArgumentException` will be thrown. Binding `ring.swagger.json-schema/*ignore-missing-mappings*` to true, one
can ignore the errors (missing schema elements will be ignored from
the generated JSON Schema).

### Model names

Prismatic Schema names are used to name the Swagger Body & Response models. Nested schemas are traversed and all found sub-schemas are generated automatically a name (so that they can be referenced in the JSON Schema). 

If multiple such schemas have same name but have different value, an describive `IllegalArgumentException` is raised. This can happen if one transforms schemas via normal `clojure.core` functions:

```clojure
(s/defschema User {:id s/Str, :name s/Str})
(def NewUser (dissoc User :id))

(meta User)
; {:name Kikka}

(meta NewUser)
; {:name Kikka} <- fail!
```

There are better schema transformers functions available at [schema-tools](https://github.com/metosin/schema-tools).

### Adding support for custom Schema elements

JSON Schema generation is supported by the `ring.swagger.json-schema/json-type` multimethod. One can register own schema types by installing new methods for it's dispatch:

```clojure
(require '[ring.swagger.json-schema :as jsons])
(require '[schema.core :as s])
(defmethod jsons/json-type s/Maybe [e] (swagger/->json (:schema e)))
```

One might also need to write both JSON Serialization for the Schema values and coercion function to de-serialize the value back from JSON.

### Extra Schema elements supported by `ring.swagger.json-schema-dirty`

Some Schema elements are impossible to accurately describe within boundaries of JSON-Schema or Swagger spec.
You can require `ring.swagger.json-schema-dirty` namespace to get implementations for `json-type` multimethod which allow
you to use some of these elements.

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

Ring-swagger utilizes [Schema coercions](http://blog.getprismatic.com/blog/2014/1/4/schema-020-back-with-clojurescript-data-coercion) for transforming the input data into vanilla Clojure and back.

```clojure
(require '[schema.core :as s])
(require '[ring.swagger.schema :refer [coerce!]])

(s/defschema Bone {:size Long, :animal (s/enum :cow :tyrannosaurus)})

(coerce! Bone {:size 12, :animal "cow"})
; => {:animal :cow, :size 12}

(coerce! Bone {:animal :sheep})
; ExceptionInfo throw+: #schema.utils.ErrorContainer{:error {:animal (not (#{:tyrannosaurus :cow} :sheep)), :size missing-required-key}, :type :ring.swagger.schema/validation}  ring.swagger.schema/coerce! (schema.clj:57)
```

Currently there are two modes for coercions: `:json` and `:query`. Both `coerce` and `coerce!` take an optional third parameter (default to `:json`) to denote which coercer to use. You can also use the two coercers directly from namespace `ring.swagger.coerce`.

#### Json-coercion

- numbers -> `Long` or `Double`
- string -> Keyword
- string -> `java.util.Date`, `org.joda.time.DateTime` or `org.joda.time.LocalDate`
- string -> `java.util.regex.Pattern`
- vectors -> Sets

#### Query-coercion:

Query-coercion extends the json-coercion with the following transformations:

- string -> Long
- string -> Double
- string -> Boolean

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
