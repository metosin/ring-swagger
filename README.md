# Ring-Swagger [![Build Status](https://travis-ci.org/metosin/ring-swagger.png?branch=master)](https://travis-ci.org/metosin/ring-swagger) [![Dependencies Status](http://jarkeeper.com/metosin/ring-swagger/status.png)](http://jarkeeper.com/metosin/ring-swagger)

[Swagger](http://swagger.io/) implementation for Clojure/Ring using [Prismatic Schema](https://github.com/Prismatic/schema) for data modeling.

- Supports both 1.2 and 2.0 Swagger Specs
- For web library developers:
  - A Schema-based contract for collecting route documentation from the web apps
  - Extendable Schema->JSON Schema conversion with out-of-the-box support for most Schema predicates
  - Common middleware for handling Schemas and Validation Errors.
  - Ring-handlers for exposing the swaggers artifacts
    - [swagger.json](https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#specification) for 2.0.
    - [Resource listing](https://github.com/swagger-api/swagger-spec/blob/master/versions/1.2.md#51-resource-listing) and [Api declarations](https://github.com/swagger-api/swagger-spec/blob/master/versions/1.2.md#52-api-declaration) for 1.2.
    - [Swagger-UI](https://github.com/swagger-api/swagger-ui) bindings. (the UI itself is jar-packaged [separately](https://github.com/metosin/ring-swagger-ui) or you can get it from [NPM](https://www.npmjs.com/package/swagger-ui))
- For web developers
  - Extendable Schema->JSON Mappings with out-of-the-box support for most common types
  - Utilities for input & output Schema validation & coercion

## Latest version

[![Clojars Project](http://clojars.org/metosin/ring-swagger/latest-version.svg)](http://clojars.org/metosin/ring-swagger)

## Web libs using Ring-Swagger

- [Compojure-Api](https://github.com/metosin/compojure-api) for Compojure
- [fnhouse-swagger](https://github.com/metosin/fnhouse-swagger) for fnhouse
- [pedastal-swagger](https://github.com/frankiesardo/pedestal-swagger) for Pedastal
- [rook](https://github.com/AvisoNovate/rook)

## Creating your own adapter

If your favourite web lib doesn't have an client adapter, you could write an it yourself. Here's howto (Swagger 2.0):

1. Create code to collect route information from your web lib and to pass them to Ring-Swagger (in Schema `ring.swagger.swagger2/Swagger`)
  - Check out the [samples](https://github.com/metosin/ring-swagger/blob/master/test/ring/swagger/swagger2_test.clj)
  - See also real sample adapter from [fnhouse-swagger](https://github.com/metosin/fnhouse-swagger/blob/master/src/fnhouse/swagger2.clj) with [tests](https://github.com/metosin/fnhouse-swagger/blob/master/test/fnhouse/swagger2_test.clj)
2. Create routes for the Swagger artifacts:
  - `swagger.json` via `ring.swagger.swagger2/swagger-json`
  - `swagger-ui` via `ring.swagger.ui/swagger-ui` (the ring handler) or `ring-swagger.ui/wrap-swagger-ui` (middleware)
3. Publish it.
4. Pull Request to list your adapter here

If you need help in adoption, feel free to ping us (try `ring-swagger` at #clojure on Freenode or email to firstname.lastname@metosin.fi)

### Adopting 1.2

Example adoption found in [fnhouse-swagger](https://github.com/metosin/fnhouse-swagger/blob/master/src/fnhouse/swagger.clj).

## Web Schemas

[Prismatic Schema](https://github.com/Prismatic/schema) is used to model the web apis. The Swagger Spec uses a deterministic
subset of JSON Schema, so not all Clojure Schema predicates are possible to use in documentation.

Below is the list of currently supported Schema predicates.

### Supported Schema elements

| Clojure | JSON Schema | Sample JSON |
| --------|-------|:------------:|
| `Integer` | integer, int32 | `1` |
| `Long`, `s/Int` | integer, int64 | `1` |
| `Double`, `Number, `s/Num`  | number, double | `1.2`
| `String`, `s/Str`, Keyword, `s/Keyword`      | string | `"kikka"`
| `Boolean`                   | boolean | `true`
| `nil`, `s/Any`              | void |
| `java.util.Date`, `org.joda.time.DateTime`  | string, date-time | `"2014-02-18T18:25:37.456Z"`, consumes also without millis: `"2014-02-18T18:25:37Z"`
| `java.util.regex.Pattern`,  | string, regex | `[a-z0-9]`
| `#"[a-z0-9]+"`              | string, pattern | `"a6"`
| `s/uuid`, `java.util.UUID`  | string, uuid | `"77e70512-1337-dead-beef-0123456789ab"`
| `org.joda.time.LocalDate`   | string, date | `"2014-02-19"`
| `(s/enum X Y Z)`       | *type of X*, enum(X,Y,Z)
| `(s/maybe X)`          | *type of X*
| `(s/both X Y Z)`       | *type of X*
| `(s/named X name)`     | *type of X*
| `(s/recursive Var)`    | *Ref to (model) Var*
| `(s/eq X)`    | *type of class of X*
| `(s/optional-key X)`    | *optional key*
| `(s/required-key X)`    | *required key*
| `s/Keyword` (as a key)  | *ignored*

- Vectors, Sets and Maps can be used as containers
  - Maps are presented as Complex Types and References. Model references are resolved automatically.
  - Nested maps are transformed automatically into flat maps with generated child references
    - Nested maps can be within valid containers (as only element - heregenous schema sequences not supported by the spec)

### Missing Schema elements

If ring-swagger can't transform the Schemas into JSON Schemas,
by default a `IllegalArgumentException` will be thrown.

Binding `ring.swagger.json-schema/*ignore-missing-mappings*` to true, one
can ingore the errors (missing schema elements will be ignored from
the generated JSON Schema).

### Schema elements supported by `ring.swagger.json-schema-dirty`

Some Schema elements are impossible to accurately describe within boundaries of JSON-Schema or Swagger spec.
You can require `ring.swagger.json-schema-dirty` namespace to get implementations for `json-type` multimethod which allow
you to use some of these elements.

Be warned that Swagger-UI might not display these correctly and the code generated by swagger-codegen will be inaccurate.

| Clojure | JSON Schema | Sample  |
| --------|-------|:------------:|
| `(s/either X Y Z)`| oneOf: *type of X*, *type of Y*, *type of Z*
| `(s/conditional pred X pred Y pred Z)` | oneOf: *type of X*, *type of X*, *type of Z*
| `(s/if pred X Y)` | oneOf: *type of X*, *type of Y*

### Currently Non-supported Schema elements

these should work, just need the mappings (feel free to contribute!):

- `s/Symbol`
- `s/Inst`

## Adding support for your own schema-types

JSON Schema generation is implemented using multimethods. You can register your own schema types by installing new methods to the multimethods.

```clojure
(require '[ring.swagger.json-schema :as jsons])
(require '[schema.core :as s])
(defmethod jsons/json-type s/Maybe [e] (swagger/->json (:schema e)))
(defmethod jsons/json-type s/Any [_] {:type "string"})
```

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
- string -> `java.util.regex.Pattern`
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
- support for Files
- full spec

## License

Copyright © 2014,2015 Metosin Oy

Distributed under the Eclipse Public License, the same as Clojure.
