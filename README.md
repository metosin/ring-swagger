# Ring-Swagger [![Build Status](https://travis-ci.org/metosin/ring-swagger.svg?branch=master)](https://travis-ci.org/metosin/ring-swagger) [![Downloads](https://versions.deps.co/metosin/ring-swagger/downloads.svg)](https://versions.deps.co/metosin/ring-swagger)

[Swagger](http://swagger.io/) 2.0 implementation for Clojure/Ring using [Plumatic Schema](https://github.com/Plumatic/schema) (support for [clojure.spec](http://clojure.org/about/spec) via [spec-tools](https://github.com/metosin/spec-tools)).

- Transforms deeply nested Schemas into Swagger JSON Schema definitions
- Extended & symmetric JSON & String serialization & coercion
- Middleware for handling Schemas Validation Errors & Publishing swagger-data
- Local api validator
- Swagger artifact generation
  - [swagger.json](https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#specification) via `ring.swagger.swagger2/swagger-json`
  - [Swagger UI](https://github.com/swagger-api/swagger-ui) bindings. (get the UI separately as [jar](https://github.com/metosin/ring-swagger-ui) or from [NPM](https://www.npmjs.com/package/swagger-ui))

## Latest version

[![Clojars Project](http://clojars.org/metosin/ring-swagger/latest-version.svg)](http://clojars.org/metosin/ring-swagger)

The [CHANGELOG](https://github.com/metosin/ring-swagger/blob/master/CHANGELOG.md).

Requires Java 1.8+

## Web libs using Ring-Swagger

- [Compojure-Api](https://github.com/metosin/compojure-api) for Compojure
- [fnhouse-swagger](https://github.com/metosin/fnhouse-swagger) for fnhouse
- [route-swagger](https://github.com/frankiesardo/route-swagger) for Pedestal
- [yada](https://github.com/juxt/yada)
- [kekkonen](https://github.com/metosin/kekkonen)

## Getting help

[Clojurians slack](https://clojurians.slack.com/) ([join](http://clojurians.net/)) has a channel [#ring-swagger](https://clojurians.slack.com/messages/ring-swagger/) for Ring-swagger related issues. You can also ask questions about Ring-swagger on other channels at Clojurians Slack or at #clojure on Freenode IRC (mention or `ring-swagger` to highlight us).

## Info

Route definitions are expected as a clojure Map defined by the Schema [Contract](https://github.com/metosin/ring-swagger/blob/master/src/ring/swagger/swagger2_schema.clj).
The Schema allows mostly any extra keys as ring-swagger tries not to be on your way - one can pass any  valid Swagger spec data in.

[API Docs](http://metosin.github.io/ring-swagger/doc/).

### Simplest possible example

```clojure
(require '[ring.swagger.swagger2 :as rs])

(rs/swagger-json {})

; {:swagger "2.0",
;  :info {:title "Swagger API", :version "0.0.1"},
;  :produces ["application/json"],
;  :consumes ["application/json"],
;  :paths {},
;  :definitions {}}
```

### More complete example

Info, tags, routes and anonymous nested schemas.

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
     :paths {"/api/ping" {:get {}}
             "/user/:id" {:post {:summary "User Api"
                                  :description "User Api description"
                                  :tags ["user"]
                                  :parameters {:path {:id s/Str}
                                               :body User}
                                  :responses {200 {:schema User
                                                   :description "Found it!"}
                                              404 {:description "Ohnoes."}}}}}}))

; {:swagger "2.0",
;  :info {:title "Sausages",
;         :version "1.0.0",
;         :description "Sausage description",
;         :termsOfService "http://helloreverb.com/terms/",
;         :contact {:name "My API Team",
;                   :email "foo@example.com",
;                   :url "http://www.metosin.fi"},
;         :license {:name "Eclipse Public License",
;                   :url "http://www.eclipse.org/legal/epl-v10.html"}},
;  :produces ["application/json"],
;  :consumes ["application/json"],
;  :tags [{:name "user", :description "User stuff"}],
;  :paths {"/api/ping" {:get {:responses {:default {:description ""}}}},
;          "/user/{id}" {:post {:summary "User Api",
;                               :description "User Api description",
;                               :tags ["user"],
;                               :parameters [{:in "path",
;                                             :name "id",
;                                             :description "",
;                                             :required true,
;                                             :type "string"}
;                                            {:in "body",
;                                             :name "User",
;                                             :description "",
;                                             :required true,
;                                             :schema {:$ref "#/definitions/User"}}],
;                               :responses {200 {:schema {:$ref "#/definitions/User"},
;                                                         :description "Found it!"},
;                                           404 {:description "Ohnoes."}}}}},
;  :definitions {"User" {:type "object",
;                        :properties {:id {:type "string"},
;                                     :name {:type "string"},
;                                     :address {:$ref "#/definitions/UserAddress"}},
;                        :additionalProperties false,
;                        :required (:id :name :address)},
;                "UserAddress" {:type "object",
;                               :properties {:street {:type "string"},
;                                                     :city {:type "string",
;                                                            :enum (:tre :hki)}},
;                               :additionalProperties false,
;                               :required (:street :city)}}}
```

producing the following ui:

![ring-swagger](https://raw.githubusercontent.com/wiki/metosin/ring-swagger/ring-swagger.png)

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

 :collection-format               - Sets the collectionFormat for query and formData
                                    parameters.
                                    Possible values: multi, ssv, csv, tsv, pipes."
```

For example, to get default response descriptions from the [HTTP Spec](http://en.wikipedia.org/wiki/List_of_HTTP_status_codes),
you can do the following:

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

The generated full spec can be validated against the [Swagger JSON Schema](https://raw.githubusercontent.com/reverb/swagger-spec/master/schemas/v2.0/schema.json)
with the help of [scjsv](https://github.com/metosin/scjsv).

```clojure
(require '[ring.swagger.validator :as v])

(v/validate (rs/swagger-json {:paths {"/api/ping" {:get nil}}}))
; nil

(v/validate (rs/swagger-json {:pathz {"/api/ping" {:get nil}}}))
; ({:level "error"
;   :schema {:loadingURI "#", :pointer ""}
;   :instance {:pointer ""}
;   :domain "validation"
;   :keyword "additionalProperties"
;   :message "object instance has properties which are not allowed by the schema: [\"pathz\"]", :unwanted ["pathz"]})
```

For more information about creating your own adapter, see [Collecting API Documentation](https://github.com/metosin/ring-swagger/wiki/Collecting-API-Documentation).

## Transforming the Swagger Spec

There are the following utility functions for transforming the spec (on the client side):

`ring.swagger.swagger2/transform-operations` - transforms the operations under the :paths of a ring-swagger spec
by applying `(f operation)` to all operations. If the function returns nil, the given operation is removed.

As an example, one can filter away all operations with `:x-no-doc` set to `true`:

```clojure
(defn remove-x-no-doc [endpoint]
  (if-not (some-> endpoint :x-no-doc true?)
    endpoint))

(transform-operations remove-x-no-doc {:paths {"/a" {:get {:x-no-doc true}, :post {}}
                                               "/b" {:put {:x-no-doc true}}}}))
; {:paths {"/a" {:post {}}}}
```

## Web Schemas

[Prismatic Schema](https://github.com/Prismatic/schema) is used to describe both the input & output schemas for routes.

As Swagger 2.0 Spec Schema is a deterministic subset of JSON Schema, so not all Clojure Schema elements can be used.

### Schema to Swagger JSON Schema conversion

There are two possible methods to do this:

1. class-based dispatch via `ring.swagger.json-schema/convert-class`.
2. protocol-based dispatch via `ring.swagger.json-schema/JsonSchema` - the `convert` fn.

Both take the Schema and swagger options map as arguments. Options contain also `:in` to denote the possible location
of the schema (`nil`, `:query`, `:header`, `:path`, `:formData` and `:body`).

To support truly symmetric web schemas, one needs also to ensure both JSON Serialization and
deserialization/coercion from JSON.

### Class-based dispatch

```clojure
(require '[ring.swagger.json-schema :as json-schema])

(defmethod json-schema/convert-class java.sql.Date [_ _] {:type "string" :format "date"})
```

#### Protocol-based dispatch

```clojure
(require '[ring.swagger.json-schema :as json-schema])

(extend-type java.util.regex.Pattern
  json-schema/JsonSchema
  (json-schema/convert [e _]
    {:type "string" :pattern (str e)}))
```

One can also use the options to create more accurate specs (via the `:in` option).

```clojure
(extend-type schema.core.Maybe
  json-schema/JsonSchema
  (convert [e {:keys [in]}]
    (let [schema (->swagger (:schema e))]
      (if (#{:query :formData} in)
        (assoc schema :allowEmptyValue true)
        schema))))
```

### Out-of-the-box supported Schema elements

| Clojure Schema                              | JSON Schema              | Sample JSON |
| --------------------------------------------|--------------------------|:-----------:|
| `Integer`                                   | integer, int32           | `1`
| `Long`, `s/Int`                             | integer, int64           | `1`
| `Double`, `Number`, `s/Num`                 | number, double           | `1.2`
| `String`, `s/Str`, `Keyword`, `s/Keyword`, `Symbol`, `s/Symbol`, `s/Any` non-body-parameter | string                   | `"kikka"`
| `Boolean`                                   | boolean                  | `true`
| `nil`, `s/Any` body-parameter            | void                     |
| `java.util.regex.Pattern`,                  | string, regex            | `[a-z0-9]`
| `#"[a-z0-9]+"`                              | string, pattern          | `"a6"`
| `s/Uuid`, `java.util.UUID`                  | string, uuid             | `"77e70512-1337-dead-beef-0123456789ab"`
| `java.util.Date`, `org.joda.time.DateTime`, `s/Inst`, `java.time.Instant` | string, date-time        | `"2014-02-18T18:25:37.456Z"`, also without millis: `"2014-02-18T18:25:37Z"`
| `org.joda.time.LocalDate`, `java.time.LocalDate` | string, date             | `"2014-02-19"`
| `org.joda.time.LocalTime`, `java.time.LocalTime` | string, time             | `"16:22"`
| `(s/enum X Y Z)`                            | *type of X*, enum(X,Y,Z)
| `(s/maybe X)`                               | *type of X*
| `(s/both X Y Z)`                            | *type of X*
| `(s/constrained X pred)`                    | *type of X*
| `(s/conditional p1 X p2 Y p3 Z)`            | *one of type X, Y, Z*
| `(s/cond-pre X Y Z)`                        | *one of type X, Y, Z*
| `(s/either X Y Z)`                          | *type of X*
| `(s/named X name)`                          | *type of X*
| `(s/one X name)`                            | *type of X*
| `(s/recursive Var)`                         | *Ref to (model) Var*
| `(s/eq X)`                                  | *type of class of X*, enum(X)
| `(s/optional-key X)`                        | *optional key*
| `(s/required-key X)`                        | *required key*
| `s/Keyword` (as a key)                      | *ignored*

- All supported types have symmetric JSON serialization (Cheshire encoders) & deserialization (Schema coercions)
- Vectors, Sets and Maps can be used as containers
- Maps are presented as Complex Types and References. Model references are resolved automatically.
  - Nested maps are transformed automatically into flat maps with generated child references
  - Maps can be within valid containers (as only element - heterogeneous schema sequences not supported by the spec)

### Missing Schema elements

If Ring-swagger can't transform the Schemas into JSON Schemas, by default a `IllegalArgumentException` will be thrown.
Setting the `:ignore-missing-mappings?` to `true` causes the errors to be ignored - missing schema elements will be
ignored from the generated Swagger schema.

### Body and Response model names

Standard Prismatic Schema names are used. Nested schemas are traversed and all found sub-schemas are named
automatically - so that they can be referenced in the generated Swagger spec.

Swagger 2.0 squashes all api models into a single global namespace, so schema name collisions can happen.
When this happens, the function defined by `:handle-duplicate-schemas-fn` option is called to resolve the collision.
By default, the collisions are ignored.

One accidental reason for schema name collisions is the use of normal `clojure.core` functions to create transformed
copies of the schemas. The normal core functions retain the original schema meta-data and by so the schema name.

```clojure
(s/defschema User {:id s/Str, :name s/Str})
(def NewUser (dissoc User :id)) ; dissoc does not remove the schema meta-data

(meta User)
; {:name User :ns user}


(meta NewUser)
; {:name User :ns user} <--- fail, now there are two User-schemas around.
```

There are better schema transformers functions available at [schema-tools](https://github.com/metosin/schema-tools).
It's an implicit dependency of ring-swagger.

### Extra Schema elements supported by `ring.swagger.json-schema-dirty`

Some Schema elements are impossible to accurately describe within boundaries of JSON-Schema or Swagger spec.
You can require `ring.swagger.json-schema-dirty` namespace to get JSON Schema dispatching for the following:

**WARNING** Swagger-UI might not display these correctly and the code generated by swagger-codegen will be inaccurate.

| Clojure | JSON Schema | Sample  |
| --------|-------|:------------:|
| `(s/conditional pred X pred Y pred Z)` | x-oneOf: *type of X*, *type of Y*, *type of Z*
| `(s/if pred X Y)` | x-oneOf: *type of X*, *type of Y*

### Schema coercion

Ring-swagger uses [Schema coercions](http://plumatic.github.io/schema-0-2-0-back-with-clojurescript-data-coercion)
for transforming the input data into vanilla Clojure and back.

There are two coercers in `ring.swagger.coerce`, the `json-schema-coercion-matcher` and `query-schema-coercion-matcher`.
These are enchanced versions of the original Schema coercers, adding support for all the supported Schema elements,
including Dates & Regexps.

#### Custom Coercions

In order to allow for custom input coercion, ring-swagger includes a multimethod 'custom-matcher' that can be implemented for custom input types. For example, to coerce currency strings into joda.money.Money objects, you can implement the following:

```clojure
(require '[ring.swagger.coerce :as coerce])
(import org.joda.money.Money)

(defmethod coerce/custom-matcher org.joda.money.Money  [_]  #(org.joda.money.Money/parse %))
```

This will allow org.joda.money.Money objects in your Schema definitions to be coerced correctly. However, this is only for coercing input, see [Schema to Swagger JSON Schema conversion](#schema-to-swagger-json-schema-conversion) for examples on transforming output.

#### Coerce!

Ring-swagger provides a convenience function for coercion, `ring.swagger.schema/coerce!`. It returns either a valid
coerced value of slingshots an Map with type `:ring.swagger.schema/validation`. One can catch these exceptions via
`ring.swagger.middleware/wrap-validation-errors` and return a JSON-friendly map of the contents.

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

One can add extra meta-data, including descriptions to schema elements using `ring.swagger.json-schema/field` and `ring.swagger.json-schema/describe` functions. These work by adding meta-data to schema under `:json-schema`-key. Objects which don't natively support meta-data, like Java classes, are wrapped automatically into `ring.swagger.json-schema/FieldSchema` to enable the meta-data.

### Example

```clojure
(require '[schema.core :as s])
(require '[ring.swagger.schema :as rs])
(require '[ring.swagger.json-schema :as rjs])

(s/defschema Required
  (rjs/field
    {(s/optional-key :name) s/Str
     (s/optional-key :title) s/Str
     :address (rjs/field
                {:street (rsjs/field s/Str {:description "description here"})}
                {:description "Streename"
                 :example "Ankkalinna 1"})}
    {:minProperties 1
     :description "I'm required"
     :example {:name "Iines"
               :title "Ankka"}}))

; produces the following JSON Schema models =>
;
; {"Required" {:type "object"
;              :description "I'm required"
;              :example {:name "Iines"
;                        :title "Ankka"}
;              :minProperties 1
;              :required [:address]
;              :properties {:name {:type "string"}
;                           :title {:type "string"}
;                           :address {:$ref "#/definitions/RequiredAddress"}}
;              :additionalProperties false}
;  "RequiredAddress" {:type "object"
;                     :description "Streename"
;                     :example "Ankkalinna 1"
;                     :properties {:street {:type "string"
;                                           :description "description here"}}
;                     :required [:street]
;                     :additionalProperties false}}
```

## License

Copyright © 2014-2018 [Metosin Oy](http://www.metosin.fi)

Distributed under the Eclipse Public License, the same as Clojure.
