# Ring-Swagger

[![Build Status](https://travis-ci.org/metosin/ring-swagger.png?branch=master)](https://travis-ci.org/metosin/ring-swagger)

[Swagger](https://helloreverb.com/developers/swagger) implementation for Ring using Prismatic [Schema](https://github.com/Prismatic/schema) for data models and coercion.

- Provides functions to create Swagger [Resource listing](https://github.com/wordnik/swagger-core/wiki/Resource-Listing), [Api declarations](https://github.com/wordnik/swagger-core/wiki/API-Declaration) and the [Swagger-UI](https://github.com/metosin/ring-swagger-ui).
- Schema extensions for modelling, coercion and Swagger [JSON Schema](http://json-schema.org/) generation
- Does **not** cover how the routes and models are actually collected from your web app
   - Provides a Map-based API for web libs to create Swagger Spec out of their route definitions

## Latest version

[![Clojars Project](http://clojars.org/metosin/ring-swagger/latest-version.svg)](http://clojars.org/metosin/ring-swagger)

## Web libs using Ring-Swagger

- [Compojure-Api](https://github.com/metosin/compojure-api) for Compojure
- [fnhouse-swagger](https://github.com/metosin/fnhouse-swagger) for fnhouse

If your favourite web lib doesn't have an client adapter, you could write an it yourself. Here's howto:

1. Define routes to serve the following:
  - **api listing**: `ring.swagger.core/api-listing`
  - **api declaration**: `ring.swagger.core/api-declaration`
  - (optionally) **swagger-ui**: `ring.swagger.ui/swagger-ui`
2. Create code to collect routes from your web lib and to pass them to Ring-Swagger fns. Sample adapter [Here](https://github.com/metosin/fnhouse-swagger/blob/master/src/fnhouse/swagger.clj)
3. Publish it.
4. Pull Request to list your adapter here

# Usage

## Models

From version `0.9.0` onwards, vanilla `schema.core/defschema` is used to define the web schemas. See [Schema](https://github.com/Prismatic/schema) for more details on building the schemas.

The Swagger/JSON Schema Spec 1.2 is more more limited compared to the Clojure Schema, so not all possible Schema predicates and structures are possible to use.

In namespace `ring.swagger.schema` there are some helpers for creating the schemas.

### Supported Schema elements

| Clojure | JSON Schema | Sample  |
| --------|-------|:------------:|
| `Long`, `s/Int`        | integer, int64 | `1`|
| `Double`                    | number, double | `1.2`
| `String`, `s/Str`, Keyword, `s/Keyword`      | string | `"kikka"`
| `Boolean`                   | boolean | `true`
| `nil`, `s/Any`              | void |
| `java.util.Date`, `org.joda.time.DateTime`  | string, date-time | `"2014-02-18T18:25:37.456Z"`, consumes also without millis: `"2014-02-18T18:25:37Z"`
| `s/uuid`, `java.util.UUID`        | string, uuid | `"77e70512-1337-dead-beef-0123456789ab"`
| `org.joda.time.LocalDate`   | string, date | `"2014-02-19"`
| `(s/enum X Y Z)`       | *type of X*, enum(X,Y,Z)
| `(s/maybe X)`          | *type of X*
| `(s/both X Y Z)`       | *type of X*
| `(s/recursive Var)`    | *Ref to (model) Var*
| `(s/eq X)`    | *type of class of X*
| `(s/optional-key X)`    | *optional key*
| `(s/required-key X)`    | *required key*
| `s/Keyword` (as a key)  | *ignored in visualizations*



- Vectors, Sets and Maps can be used as containers
  - Maps are presented as Complex Types and References. Model references are resolved automatically.
  - Nested maps are transformed automatically into flat maps with generated child references
    - Nested maps can be within valid containers (as only element - heregenous schema sequences not supported by the spec)

### Currently Non-supported Schema elements

- `s/either` (can't work with the swagger 1.2 json schema?)
- `s/conditional`
- `s/if`

these should work, just need the mappings (feel free to contribute!):

- `s/Symbol`
- `s/Inst`
- `s/Regex`

### Schema coercion

Ring-swagger utilizes [Schema coercions](http://blog.getprismatic.com/blog/2014/1/4/schema-020-back-with-clojurescript-data-coercion) for transforming the input data into vanilla Clojure and back.

```clojure
(require '[schema.core :as s])
(require '[ring.swagger.schema :refer [coerce!]])

(s/defschema Bone {:size Long
                   :animal (s/enum :cow :tyrannosaurus)})

(coerce! Bone {:size 12
               :animal :cow})
; {:animal :cow, :size 12}

(coerce! Bone {:animal :sheep})
; ExceptionInfo throw+: {:type :ring.swagger.schema/validation, :error {:animal (not (#{:tyrannosaurus :cow} :sheep)), :size missing-required-key}}  ring.swagger.schema/coerce! (schema.clj:114)
```

There are two modes for coercions: `:json` and `:query`. Both `coerce` and `coerce!` take an optional third parameter (default to `:json`) to denote which coercer to use. You can also use the two coercers directly from namespace `ring.swagger.coerce`.

#### Json-coercion

- numbers -> `Long` or `Double`
- string -> Keyword
- string -> `java.util.Date`, `org.joda.time.DateTime` or `org.joda.time.LocalDate`
- vectors -> Sets

#### Query-coercion:

extends the json-coercion with the following transformations:

- string -> Long
- string -> Double
- string -> Boolean

### A Sample Model usage

```clojure
(require '[ring.swagger.schema :refer [coerce coerce!]])
(require '[schema.core :as s])

(s/defschema Country {:code (s/enum :fi :sv)
                      :name String})
; #'user/Country

(s/defschema Customer {:id Long
                       :name String
                       (s/optional-key :address) {:street String
                                                  :country Country}})
; #'user/Customer

Country
; {:code (enum :fi :sv), :name java.lang.String}

Customer
; {:id java.lang.Long, :name java.lang.String, #schema.core.OptionalKey{:k :address} {:street java.lang.String, :country {:code (enum :fi :sv), :name java.lang.String}}}

(def matti {:id 1 :name "Matti Mallikas"})
(def finland {:code :fi :name "Finland"})

(coerce Customer matti)
; {:name "Matti Mallikas", :id 1}

(coerce Customer (assoc matti :address {:street "Leipätie 1":country finland}))
; {:address {:country {:name "Finland", :code :fi}, :street "Leipätie 1"}, :name "Matti Mallikas", :id 1}

(coerce Customer {:id 007})
; #schema.utils.ErrorContainer{:error {:name missing-required-key}}

(coerce! Customer {:id 007})
; ExceptionInfo throw+: {:type :ring.swagger.schema/validation, :error {:name missing-required-key}}  ring.swagger.schema/coerce! (schema.clj:89)
```

## Creating your own schema-types

JSON Schema generation is implemented using multimethods. You can register your own schema types by installing new methods to the multimethods.

```clojure
(require '[ring.swagger.json-schema :as jsons])
(require '[schema.core :as s])
(defmethod jsons/json-type s/Maybe [e] (swagger/->json (:schema e)))
(defmethod jsons/json-type s/Any [_] {:type "string"})
```

## Describing schemas

You can add e.g. description to you schemas using `ring.swagger.schema/field` and `ring.swagger.schema/describe` functions.
These work by adding meta-data to schema under `:json-schema`-key. Objects which don't support meta-data, like Java classes, are
wrapped into `s/both`.

```clojure
(s/defschema Customer {:id Long
                       :name (describe String "the name")
                       (s/optional-key :address) (describe {:street String
                                                            :country Country}
                                                           "The Address")})

(= (jsons/json-schema-meta (describe Customer "The Customer")) {:description "The Customer"})
```

## TODO

- web schema validation ("can this be transformed to json & back")
- pluggable web schemas (protocol to define both json generation & coercion)
- authorization
- support for Files
- full spec

## License

Copyright © 2014 Metosin Oy

Distributed under the Eclipse Public License, the same as Clojure.
