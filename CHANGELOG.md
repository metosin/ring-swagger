## NEXT

* Fix memory leaks via multimethods caching default dispatch values: https://github.com/metosin/compojure-api/issues/454

## 0.26.2 (1.4.2019)

* `s/eq` is mapped into enum by [Joel Kaasinen](https://github.com/opqdonut).

* Updated deps:

```clj
[cheshire "5.8.1"] is available but we use "5.8.0"
[metosin/ring-http-response "0.9.1"] is available but we use "0.9.0"
[ring/ring-core "1.7.1"] is available but we use "1.6.3"
[metosin/schema-tools "0.11.0"] is available but we use "0.10.3"
[prismatic/schema "1.1.10"] is available but we use "1.1.9"
[metosin/scjsv "0.5.0"] is available but we use "0.4.1"
[clj-time "0.15.1"] is available but we use "0.14.4"
```

## 0.26.1 (15.6.2018)

Updated dependencies:

```clj
[metosin/schema-tools "0.10.3"] is available but we use "0.9.1"
[prismatic/schema "1.1.9"] is available but we use "1.1.7"
[clj-time "0.14.4"] is available but we use "0.14.2"
[potemkin "0.4.5"] is available but we use "0.4.4"
[frankiesardo/linked "1.3.0"] is available but we use "1.2.9"
```

## 0.26.0 (16.2.2018)

* **BREAKING**: drop automatic (potemkin) imports of `ring.middleware.multipart-params` from `ring.swagger.upload` to support non-servlet servers. You can require those manually.

* Updated deps

```clj
[metosin/scjsv "0.4.1"] is available but we use "0.4.0"
```

## 0.25.0 (9.1.2018)

* **BREAKING**: requires Java 1.8+
* Cleanup transitive dependency conflicts
* Support both swagger-ui major versions (2.* and 3.*)

## 0.24.4 (11.12.2017)

* Updated deps

```clj
[prismatic/plumbing "0.5.5"] is available but we use "0.5.4"
[ring/ring-core "1.6.3"] is available but we use "1.6.2"
[clj-time "0.14.2"] is available but we use "0.14.0"
```

## 0.24.3 (24.10.2017)

* Emit `:x-oneOf` and `:x-oneOf` instead of `:oneOf` & `:anyOf` for Swagger2, thanks to [Nicolas Ha](https://github.com/nha)

```clj
[metosin/schema-tools "0.9.1"] is available but we use "0.9.0"
[prismatic/schema "1.1.7"] is available but we use "1.1.6"
```

## 0.24.2 (24.8.2017)

* `:description` is read from Schema meta-data into response bodys too. Thanks to [David James Humphreys](https://github.com/davidjameshumphreys)

```clj
{:get {:parameters {:query {:kikka/kukka String}}
       :responses {200 (rsjs/describe
                         {:body {:kikka/kukka String}}
                         "A meta description")}}}
```

* Easy to define custom coercions via multimethod, thanks to [Nick Bailey](https://github.com/nickmbailey)

```clojure
(require '[ring.swagger.coerce :as coerce])
(import org.joda.money.Money)

(defmethod coerce/custom-matcher org.joda.money.Money  [_]  #(org.joda.money.Money/parse %))
```

* updated deps:

```clj
[cheshire "5.8.0"] is available but we use "5.7.1"
[potemkin "0.4.4"] is available but we use "0.4.3"
```

## 0.24.1 (25.7.2017)

* Support for `s/Num` coercion, thanks to [Valtteri Harmainen](https://github.com/vharmain).

* updated dependencies:

```clj
[ring/ring-core "1.6.2"] is available but we use "1.6.0"
[prismatic/schema "1.1.6"] is available but we use "1.1.5"
[clj-time "0.14.0"] is available but we use "0.13.0"
```

## 0.24.0 (11.5.2017)

* Drop `[slingshot "0.12.2"]` dependency, use `ex-info` instead.
* Support qualified keywords in swagger parameters, fixes [#93](https://github.com/metosin/ring-swagger/issues/93)
* Depend directly on `[ring/ring-core "1.6.0"]`

* updated dependencies:

```clj
[ring/ring-core "1.6.0"] is available but we use "1.6.0-beta7"
[cheshire "5.7.1"] is available but we use "5.7.0"
[prismatic/plumbing "0.5.4"] is available but we use "0.5.3"
[prismatic/schema "1.1.5"] is available but we use "1.1.3"
[metosin/ring-http-response "0.9.0"] is available but we use "0.8.2"
[metosin/ring-swagger-ui "2.2.10"] is available but we use "2.2.8"
```

## 0.23.0 (10.2.2017)

* **BREAKING**: Requires Java 1.7
* **BREAKING**: `ring.swagger.ui` is now `ring.swagger.swagger-ui` with new map-based options:
  - `swagger-ui` takes just a single options map instead of varargs with new key `path` for the path to be mounted
  - `wrap-swagger-ui` takes just a single options map (with `path`) instead of varargs
  - both support the new async-ring
  - Fixes [#99](https://github.com/metosin/ring-swagger/issues/99)

```clj
(require '[ring.swagger.swagger-ui :as ui])

(ui/swagger-ui {:path "/api-docs"})

(-> app (ui/wrap-swagger-ui {:path "/api-docs"}))
```

* Support async-ring in `ring.swagger.middleware`
* Support for `java.io.File` return type, mapping to `"file"`
* Use `ex-info` & `ex-data` instead of Slingshot in exceptions

* updated dependencies:

```clj
[cheshire "5.7.0"] is available but we use "5.6.3"
[metosin/ring-http-response "0.8.2"] is available but we use "0.8.1"
```

* droped dependencies:

```clj
[slingshot "0.12.2"]
```

## 0.22.14 (11.1.2017)

* updated dependencies:

```clj
[metosin/ring-http-response "0.8.1"] is available but we use "0.8.0"
```

## 0.22.13 (7.1.2017)

* produce json schema properties also from non-keyword keys
* updated dependencies:

```clj
[clj-time "0.13.0"] is available but we use "0.12.1"
```

## 0.22.12 (31.10.2016)

* Support for Java 1.8 Dates, fixes [#107](https://github.com/metosin/ring-swagger/issues/107). Thanks to [Matt K](https://github.com/mtkp) for contributing!
* updated dependencies:

```clj
[metosin/scjsv "0.4.0"] is available but we use "0.3.0"
[clj-time "0.12.1"] is available but we use "0.12.0"
[frankiesardo/linked "1.2.9"] is available but we use "1.2.8"
```

## 0.22.11 (27.9.2016)

* Validating a schema does not load anything from the Internet anymore, fixes [#113](https://github.com/metosin/ring-swagger/issues/113).
* updated dependencies:

```clj
[frankiesardo/linked "1.2.8"] is available but we use "1.2.7"
```

## 0.22.10 (29.8.2016)

* Support for `schema.core/defrecords`, fixes [#103](https://github.com/metosin/ring-swagger/issues/103).
* Support for primitives & arrays of primitives both in `:body` & `:response`.
   * Replaces [#56](https://github.com/metosin/ring-swagger/pull/56)
   * Fixes [#55](https://github.com/metosin/ring-swagger/issues/55)
   * Fixes [compojure-api/#177](https://github.com/metosin/compojure-api/issues/177)
   * Thanks to [Tim Gilberd](https://github.com/timgilbert) for the initial fix!
* Support for top-level Schema-records for non-body-parameters, fixes [#104](https://github.com/metosin/ring-swagger/issues/104).
   * Thanks to [quimrstorres](https://github.com/quimrstorres)!
* Fixed to work Clojure 1.9 alpha11, thanks to [Brent Hagany](https://github.com/bhagany)
* Support for `org.joda.time.LocalTime`, thanks to [Francesco Bellomi](https://github.com/fbellomi)!
* Fixes all reflection warnings
* `s/Any` as a value generates empty object `{}` to JSON Schema, Fixes #91, thanks to [Mika Haapakorpi](https://github.com/hkorpi) for the tip.

```clj
[cheshire "5.6.3"] is available but we use "5.6.1"
[metosin/ring-http-response "0.8.0"] is available but we use "0.7.0"
[metosin/scjsv "0.3.0"] is available but we use "0.2.0"
[prismatic/schema "1.1.3"] is available but we use "1.1.2"
[frankiesardo/linked "1.2.7"] is available but we use "1.2.6"
```

## 0.22.9 (14.6.2016)

* Map-schema swagger-metadata is set to target schema-object, not for the `$ref` of it, fixes [#96](https://github.com/metosin/ring-swagger/issues/96).
* Better support for additional json-schema meta-data via `ring.swagger.json-schema/field`:

```clj
(s/defschema Required
  (rsjs/field
    {:name s/Str
     :title s/Str
     :address (rsjs/field
                (rsjs/field s/Str {:description "description here"})
                {:description "Streename"
                 :example "Ankkalinna 1"})}
    {:minProperties 1
     :description "I'm required"
     :example {:name "Iines"
               :title "Ankka"}}))
```

* Better support for nillable fields via `x-nullable` [standard hack](https://github.com/OAI/OpenAPI-Specification/issues/229), fixes fixes [#97](https://github.com/metosin/ring-swagger/issues/97)

* updated dependencies:

```clj
[metosin/ring-http-response "0.7.0"] is available but we use "0.6.5"
[prismatic/schema "1.1.2"] is available but we use "1.1.1"
[clj-time "0.12.0"] is available but we use "0.11.0"
```

## 0.22.8 (21.5.2016)

* Response headers are mapped correctly, Fixes https://github.com/metosin/compojure-api/issues/232.

* updated dependencies:

```clj
[prismatic/schema "1.1.1"] is available but we use "1.1.0"
```

## 0.22.7 (24.4.2016)

* `ring.swagger.core/with-named-subschemas` retains metadata, fixes https://github.com/metosin/compojure-api/issues/168

- update dependencies:

```clj
[prismatic/schema "1.1.0"] is available but we use "1.0.5"
[cheshire "5.6.1"] is available but we use "5.5.0"
[prismatic/plumbing "0.5.3"] is available but we use "0.5.2"
```

## 0.22.6 (20.3.2016)

**[compare](https://github.com/metosin/ring-swagger/compare/0.22.5...0.22.6)**

* updated dependencies:

```clj
[metosin/schema-tools "0.9.0"] is available but we use "0.8.0"
```

## 0.22.5 (17.3.2016)

**[compare](https://github.com/metosin/ring-swagger/compare/0.22.4...0.22.5)**

* Use strings instead of keywords in `:in`, fixes [#88](https://github.com/metosin/ring-swagger/issues/88), thanks to https://github.com/preoctopus for the fix.
* experimental `ring.swagger.swagger2-full-schema/Swagger` for validating the ring-swagger spec in Clojure

* updated dependencies:

```clj
[prismatic/schema "1.0.5"] is available but we use "1.0.4"
[metosin/schema-tools "0.8.0"] is available but we use "0.7.0"
```

## 0.22.4 (11.2.2016)

**[compare](https://github.com/metosin/ring-swagger/compare/0.22.3...0.22.4)**

* Closed Map Schemas (e.g. `s/find-extra-keys-schema` returning nil) will have `:additionalProperties` set to `false`.
* Path-parameters are always required (in align to the spec)
* Body-parameters are not required if wrapped in `schema.core.Maybe`
* `ring.swagger.coerce/query-coercions` now apply both `:json` and `:query` coercions for `s/Int`, `Long` and `Double`
* Cleaned up internals
  * `ring.swagger.swagger2/transform` -> `ring.swagger.json-schema/schema-object`

## 0.22.3 (17.1.2016)

**[compare](https://github.com/metosin/ring-swagger/compare/0.22.2...0.22.3)**

- Fixed generating Swagger path templates in cases where path parameter is followed
by an extension ([#82](https://github.com/metosin/ring-swagger/issues/82))

- Make the JSON Schema validator public: `ring.swagger.validator/validate`.

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

## 0.22.2 (13.1.2016)

**[compare](https://github.com/metosin/ring-swagger/compare/0.22.1...0.22.2)**

- Discard all extra map keys from properties ([#77](https://github.com/metosin/ring-swagger/issues/77))
- All Schema [extra keys](https://github.com/Prismatic/schema/blob/master/src/cljx/schema/core.cljx#L765)
are now exposed as Swagger additional properties.
    - Previously only `s/Keyword` were supported.
- Fix JSON Schema `nil` default value ([#79](https://github.com/metosin/ring-swagger/issues/79))
- Updated dependencies:

```clj
[prismatic/schema "1.0.4"] is available but we use "1.0.3"
[potemkin "0.4.3"] is available but we use "0.4.1"
```

## 0.22.1 (29.11.2015)

**[compare](https://github.com/metosin/ring-swagger/compare/0.22.0...0.22.1)**

- Generate `(s/maybe s/Str)`-parameters of `s/Any` for non-body-parameters. Fixes [#74](https://github.com/metosin/ring-swagger/issues/74).
- Mappings for `s/Symbol` & `s/Inst`
- Use `:default` metadata of `optional-key`s set by Plumbing for Swagger
property `default` field.

```clojure
[prismatic/plumbing "0.5.2] is available but we use "0.5.1"
```

## 0.22.0 (8.11.2015)

**[compare](https://github.com/metosin/ring-swagger/compare/0.21.0...0.22.0)**

- **BREAKING**: Dropped support for Clojure 1.6
- **BREAKING**: Supports and depends on Schema 1.0.
- Uses now [linked](https://github.com/frankiesardo/linked) instead of
[ordered](https://github.com/amalloy/ordered) for maps where order matters
- Fixed [#64](https://github.com/metosin/ring-swagger/issues/64), use first
found schema name to be consistent with Json Schema.
- Fixed [#67](https://github.com/metosin/ring-swagger/issues/67) `swagger-ui`
now supports passing arbitrary options to `SwaggerUI`
- updated dependencies:

```clojure
[metosin/schema-tools "0.7.0"] is available but we use "0.5.2"
[prismatic/schema "1.0.3"] is available but we use "0.4.4"
[prismatic/plumbing "0.5.1"] is available but we use "0.4.4"
```

## 0.21.0 (1.9.2015)

**[compare](https://github.com/metosin/ring-swagger/compare/0.20.4...0.21.0)**

### Breaking changes

- **BREAKING**: Swagger 1.2 is no more supported.

- **BREAKING**: `ring.swagger.json-schema/json-type` multimethod is removed
  - will cause compile-time errors for those who have client-side custom extensions
  - new way of doing the Schema -> Swagger Schema mappings:
    - Classes via `ring.swagger.json-schema/convert-class` multimethod, taking both the class and swagger options
    - Objects (e.g. records) via `ring.swagger.json-schema/JsonSchema` protocol.

- lot's of internal cleanup in `ring.swagger.json-schema`:
  - `->json` is now `->swagger` and takes options map instead of kwargs.
  - removed option `:top` (required only for Swagger 1.2)
  - new option `:in` denote the parameter type (`:query`, `:header`, `:path`, `:formData` or `:body`)
     - responses don't have `:in`.

### New features

- Support for collections in query and form parameters (even with single parameter):
  - Parameters `{:query {:x [Long]}}` with `ring.middleware.params/wrap-params` middleware and query-string of
  `x=1&x=2&x?3` with `ring.swagger.schema/coercer!` should result in `x` being `[1 2 3]`
    - Same with Compojure-api: `:query-params [x :- [Long]]`
  - For now, only supports [collectionFormat](https://github.com/swagger-api/swagger-spec/blob/master/versions/2.0.md#fixed-fields-7)
  `multi`.

- (From compojure-api) Support for file uploads.
  - `ring.swagger.upload/TempFileUpload` and `ByteArrayUpload` Schemas to be used
  with `ring.middleware.multipart-params` default stores.

- support for `schema.core.One` by [Steffen Dienst](https://github.com/smee).

- `:version` is not mandatory field in ring-swagger schema (defaults to `0.0.1`)

- new public api fns in `ring.swagger.swagger2`:
   - `transform-operations` for generic operation transformations on the client side
   - `ensure-body-and-response-schema-names` to fix the generated schema names on the client side (vs. the `swagger-json`
   generating new names for all the requests.

- Fixes [54](https://github.com/metosin/ring-swagger/issues/54): `:paths` order is now preserved
  - use `flatland.ordered.map/ordered-map` in the client side to keep the order.

- updated dependencies:

```clojure
[metosin/schema-tools "0.5.2"] is available but we use "0.4.1"
[metosin/ring-http-response "0.6.5"] is available but we use "0.6.2"
[metosin/ring-swagger-ui "2.1.2"] is available but we use "2.0.24"
[prismatic/schema "0.4.4"] is available but we use "0.4.3"
[cheshire "5.5.0"] is available but we use "5.4.0"
[org.flatland/ordered "1.5.3"] is available but we use "1.5.2"
[clj-time "0.11.0"] is available but we use "0.9.0"
[potemkin "0.4.1"] is available but we use "0.3.13"
```

- dev-dependencies:

```clojure
[lein-ring "0.9.6"] is available but we use "0.9.4"
[funcool/codeina "0.3.0"] is available but we use "0.1.0"
[midje "1.7.0"] is available but we use "1.7.0-SNAPSHOT"
```

## 0.20.4 (25.5.2015)

- `ring.swagger.swagger2/transform-paths` for generic endpoint tranformations.
- updated dependencies:

```clojure
[metosin/schema-tools "0.4.1"] is available but we use "0.4.0"
[metosin/ring-http-response "0.6.2"] is available but we use "0.6.1"
[prismatic/schema "0.4.3"] is available but we use "0.4.2"
[prismatic/plumbing "0.4.4"] is available but we use "0.4.3"
```

- removed dependency:

```
[instar "1.0.10" :exclusions [org.clojure/clojure com.keminglabs/cljx org.clojure/clojurescript]]]
```

## 0.20.3 (17.5.2015)

- new option `:handle-duplicate-schemas-fn` to handle duplicates schemas.
  - **breaking**: default behaviour is "take the first definition" instead of `IllegalArgumentException`
  (the code was broken, did not work with anonymous predicate schemas & regexps.
- updated deps:

```clojure
[prismatic/plumbing "0.4.3"] is available but we use "0.4.2"
[lein-ring "0.9.4"] is available but we use "0.9.3"
```

## 0.20.2 (2.5.2015)

- add `:type "object"` to Swagger 2.0 Definitions, requested by [Ron](https://github.com/webron).
- Middlewares can define extra swagger data, set into a request, read out by swagger-docs
  - setting data via `set-swagger-data`, reading data via `get-swagger-data`.
  - `wrap-swagger-data` middleware for easy publishing of swagger-data.
- updated dependencies:

```clojure
[prismatic/schema "0.4.2"] is available but we use "0.4.1"
```

## 0.20.1 (26.4.2015)

- `swagger-json` now handles `nil` options, thanks to [Frankie Sardo](https://github.com/frankiesardo).
- updated dependencies:
```clojure
[prismatic/schema "0.4.1"] is available but we use "0.4.0"
```

## 0.20.0 (21.4.2015)

- welcome back the `Info` schema in `ring.swagger.swagger2-schema`!
- ring-swagger defaults in `swagger-json` are deep-merged in instead of plain merge.
- **breaking**: default spec-location has changed from `/api/api-docs` to `/swagger.json` in `ring.swagger.ui

## 0.19.6 (19.4.2015)

- initial support for `additionalProperties` via `s/Keyword` -key in the schemas. Thanks for [Juan Patten](https://github.com/runningskull/ring-swagger/commit/4f113100923414d9d8f22a862c466abdca4b788d) for the initial code.

## 0.19.5 (13.4.2015)

- throw `IllegalArgumentException` if multiple different schemas have a same name. Fixes [#39](https://github.com/metosin/ring-swagger/issues/39)
- drop import of `javax.servlet ServletContext`, causing reflection on Servlet Apps.
- updated dependencies:

```clojure
[prismatic/plumbing "0.4.2"] is available but we use "0.4.1"
[metosin/schema-tools "0.4.0"] is available but we use "0.3.0"
```

## 0.19.4 (8.4.2015)

- minify Ring-Swagger 2.0 Schema - just the [essentials](https://github.com/metosin/ring-swagger/blob/master/src/ring/swagger/swagger2_schema.clj)
- Swagger2.0 JSON Schema is now in classpath `ring/swagger/v2.0_schema.json`
- exclude ill transitive dependencies from Instar (cljx, cljs)
- use ~1000x faster JSON Schema validator in tests:

```clojure
[metosin/scjsv "0.2.0"] is available
```

## 0.19.3 (31.3.2015)

- `:resourcePath` is now set correctly with Swagger 1.2 endpoints (fixes [#36](https://github.com/metosin/ring-swagger/issues/36).

## 0.19.2 (31.3.2015)

- avoid reflection with regexps, thanks to [Michael Blume](https://github.com/MichaelBlume)
- default 2.0 responses don't have schemas
- support for walking over Schema predicates support for 1.2 too

## 0.19.1 (25.3.2015)

- `with-named-sub-schemas` takes an optional parameter - prefix for schema names
- Schema-names are now generated as String instead of Keywords in the swagger-json 2.0
- uses `[metosin/schema-tools "0.3.0]` to walk over Schema records both to fetch schema names and give names to subschemas (Swagger 2.0 only)
  - see [tests](https://github.com/metosin/ring-swagger/blob/master/test/ring/swagger/swagger2_test.clj) - in the end of the file

```clojure
[lein-ring "0.9.3"] is available but we use "0.9.2"
```

## 0.19.0 (20.3.2015)

- `with-named-sub-schemas` has learned how to add names to schemas inside `s/maybe` and others
  - This means that you can now use inline schemas inside `s/maybe`
- updated deps:

```clojure
[prismatic/schema "0.4.0"] is available but we use "0.3.7"
[prismatic/plumbing "0.4.1"] is available but we use "0.3.7"
[potemkin "0.3.12"] is available but we use "0.3.11"
[metosin/ring-http-response "0.6.1"] is available but we use "0.6.0"
```
- Fixed compatibility with Schema 0.4.0

## 0.18.1 (2.3.2015)

- Fixed wrap-validation-errors defaults

## 0.18.0 (2.3.2015)
- fixed Swagger 2.0 response bug [#29](https://github.com/metosin/ring-swagger/issues/29)
- `ring.swagger.swagger2/swagger-json` now takes an optional extra argument, the Options-map
  with options `:ignore-missing-mappings?` & `:default-response-description-fn`
- ensured that ignoring missing mappings works for both body- & non-body parameters
- **BREAKING**:
  - moved binding of `s/either` from `ring.swagger.json-schema-dirty` to `ring.swagger.json-schema`.
  Uses the first schema element as the source for the mappings
  - `ring.swagger.middleware/wrap-validation-error` takes now options as a single map
- updated deps:

```clojure
[metosin/ring-http-status "0.6.0"] is available but we use "0.5.2"
[prismatic/schema "0.3.7"] is available but we use "0.3.3"
[prismatic/plumbing "0.3.7"] is available but we use "0.3.5"
[lein-ring "0.9.2"] is available but we use "0.9.1"
```

## 0.17.0 (15.1.2015)
- revert default spec location to `/api/docs` to be more backwards compatible. Swagger2-clients should use `swagger.json`.
- `ring.swagger.middleware/comp-mw` to make middleware parametrization easier:

```clojure
(def wrap-swagger2-ui
  (comp-mw wrap-swagger-ui :swagger-docs "swagger.json"))
```

## 0.16.0 (14.1.2015)

- Swagger 2.0 support!!
  - new namespace `ring.swagger.swagger2` with clean public API with Schemas
- **breaking change**: default spec location changed from `/api/docs` to `/swagger.json` (2.0 standard)
- **breaking change**: in `ring.swagger.middleware` the `catch-validation-error` is now `wrap-validation-error`
  - takes an extra option: `:error-handler` to allow error response customization & `:catch-core-errors?` for
    catching `:schema.core/error`s (defaults to `false`).
- one can now plug own coercers for `coerce` and `coerce!`
- use real swagger json schema validator for tests (`ring.swagger.validator`)
- potential **breaking changes** for library developers due massive refactoring
- support for Regexp schemas:
   - `java.util.regex Pattern` / `s/Regex` (as a class)
   - `#"^[a-9]$"` (as a instance)
- support for 1.2 spec `authorization` parameters by [Dmitry Groshev](https://github.com/si14)
- oauth2-configs for swagger-ui by [Dmitry Groshev](https://github.com/si14)
- updated dependencies:
```clojure
[cheshire "5.4.0"] is available but we use "5.3.1"
[clj-time "0.9.0"] is available but we use "0.8.0"
*[metosin/ring-swagger-ui "2.0.24"] is available but we use "2.0.17"
*[com.github.fge/json-schema-validator "2.2.6"] is available but we use "2.2.5"
*[lein-ring "0.9.1"] is available but we use "0.8.13"
[instar "1.0.10"]
```

## 0.15.0 (8.12.2014)

- **new feature**: binding dynamic variable `ring.swagger.json-schema/*ignore-missing-mappings*`
  to true will cause unknown json-schema mappings to be ignored
- fixed #42
- updated dependencies:
```clojure
[metosin/ring-http-response "0.5.2"] is available but we use "0.5.1"
[prismatic/schema "0.3.3"] is available but we use "0.3.2"
[ring/ring-core "1.3.2"] is available but we use "1.3.1"
```

## 0.14.1 (11.10.2014)

- support for `s/Named`
- use `[org.tobereplaced/lettercase "1.0.0"]` in favour of `[camel-snake-kebab "0.2.5"]`
- update deps:
```clojure
[prismatic/schema "0.3.2"] is available but we use "0.3.1"
```

## 0.14.0 (29.10.2014)

- support for `java.lang.Number`, `java.lang.Integer`, `s/Num`
- Updated deps:
```clojure
[slingshot "0.12.1"] is available but we use "0.12.0"
[metosin/ring-http-response "0.5.1"] is available but we use "0.5.0"
[prismatic/plumbing "0.3.5"] is available but we use "0.3.4"
[camel-snake-kebab "0.2.5"] is available but we use "0.2.4"
[potemkin "0.3.11"] is available but we use "0.3.10"
[lein-ring "0.8.13"] is available but we use "0.8.11"
[prismatic/schema "0.3.1"] is available but we use "0.2.6"
```

## 0.13.0 (4.9.2014)

- Updated dependencies
  - camel-snake-kebab 0.2.0 renamed the ns `camel-snake-kebab` to `camel-snake-kebab.core`
- `ring.swagger.json-schema-dirty` namespace now provides experimental
implementation for `s/if`, `s/conditional` and `s/either` Schema transformations.
- Fixed a bug with `s/recursive`

## 0.12.0 (19.8.2014)

- Should now keep order of properties in Schemas if using `ordered-map`
  - Use `ordered-map` from [flatland.ordered.map](https://github.com/flatland/ordered)
  - `(s/defschema (ordered-map :a String ...))`

## 0.11.0 (10.8.2014)

- Removed `ring.swagger.schema/defmodel`, please use `schema.core/defschema`.
- Split JSON Schema generation to a `json-schema` module.
- Refactored Json schema transformations
  - New `describe` helper:
    - Instead of `(field Long {:description "The description"})` you can use
    - `(describe Long "The description")`

## 0.10.6 (9.7.2014)

- `ring.swagger.ui/swagger-ui` always set content-type of `application/javascript` for `conf.js`
- `ring.swagger.ui/wrap-swagger-ui` to package swagger-ui as middleware.
- updated deps:

```
[prismatic/plumbing "0.3.3"] is available but we use "0.3.2"
[metosin/ring-swagger-ui "2.0.17"] is available but we use "2.0.16-3"
2 artifacts were upgraded.
```

## 0.10.5 (28.6.2014)

- support for `s/Uuid`, thanks to @phadej!

## 0.10.3 (28.6.2014)

- support for `header`-params.
- change `s/defn` to `sm/defn, might fix #12 (https://github.com/Prismatic/schema/issues/21)

## 0.10.2 (22.6.2014)

- allow primitives as return types (fixes #9)

## 0.10.1 (20.6.2014)

- fix bug: with-named-sub-schemas does not fail with `s/Keyword`s.
- updated deps:

```clojure
[prismatic/schema "0.2.4"] is available but we use "0.2.3"
[prismatic/plumbing "0.3.2"] is available but we use "0.3.1"
[lein-ring "0.8.11"] is available but we use "0.8.10"
```

## 0.10.0 (18.6.2014)

- support for ResponseModels
- support for `s/Any` (maps to `void` as a return type, fields of this type are ignored)
- support for `s/Keyword` (or any other `schema.core.Predicate`) as a key -> fields are ignored

## 0.9.1 (17.6.2014)

- 'cos the deployment of `0.9.0` failed to clojars

## 0.9.0 (17.6.2014)

- `defmodel` with nested anonymous submodels works now with AOT
- `defmodel` doesn't contain the `model` metadata -> removed all the model var-resolutions
- support for vanilla `schema.core/defschema`s as the models
- update to use `[ring "1.3.0"]

## 0.8.8 (1.6.2014)

- `defmodel` now supports anonymous maps within valid containers (`set`,`list`,`vector`)

```clojure
(require '[ring.swagger.schema :refer :all])
(require '[ring.swagger.core :refer [transform]])

(defmodel Items {:data [{:id Long
                         :name String}]})

Items
;; {:data [{:name java.lang.String, :id java.lang.Long}]}

ItemsData
;; {:name java.lang.String, :id java.lang.Long}

(transform Items)
;; {:properties {:data {:items {:$ref "ItemsData"}, :type "array"}}, :required [:data]}

(transform ItemsData)
;; {:properties {:name {:type "string"}, :id {:type "integer", :format "int64"}}, :required [:name :id]}
```

## 0.8.7 (12.5.2014)

- Added type hints where `lein check` suggested
- Added swagger-ui tests for multiple envinronments
  - Fixed swagger-ui for servlet envinronment

## 0.8.6 (9.5.2014)

- new `ring.swagger.ui`-ns provided `swagger-ui` function which can be used to create ring handler to serve Swagger-ui.
- updated clojure dependency

## 0.8.5 (7.5.2014)

- both `consumes` and `produces` can be set by the client side for the api declaration.

## 0.8.4 (9.4.2014)

- updated docs
- `strict-schema` and `loose-schema`
- `extract-basepath` renamed to `basepath`, understands the `:servlet-context` and header `x-forwarded-proto`

## 0.8.3 (25.3.2014)

- fix bug in `string-path-parameters`

## 0.8.2 (25.3.2014)

- path parameters are not set implicitly, clients can use `(string-path-parameters url)` to create those automatically

## 0.8.1 (25.3.2014)

- json schema transformations are now all multimethod-based.
- support for `s/eq`
- support for `nil`
- `type-of` and `return-type-of` are removed in favour of `->json`.
- Route uri is passed as a String (`/api/users/:id`) instead of a Vector (`["/api/users/" :id]`)
- Parameters are passed in Schema-format, not as Json Schema

## 0.7.4 (10.3.2014)

- `resolve-model-vars` uses Walking to preserve the original collection form (Sets)
- `return-type-of` preserves Sets.

## 0.7.3 (6.3.2014)

- support for Recursive Models (`s/recursive`), thanks to [Arttu Kaipiainen](https://github.com/arttuka)!
- coersion has now two modes: `:json` and `:query`, latter converts strings to Longs, Doubles and Booleans

## 0.7.2 (4.3.2014)

- sub-models don't have `_` to split up classes. `Customer_Address` -> `CustomerAddress`. Looks good.

## 0.7.1 (3.3.2014)
- `defmodel` now supports nested maps (by generating sub-types)

```clojure
(defmodel Customer {:id String
                    :address {:street String
                              :zip Long
                              :country {:code Long
                                        :name String}}})

;; Customer => {:id java.lang.String, :address {:street java.lang.String, :zip java.lang.Long, :country {:name java.lang.String, :code java.lang.Long}}}
;; Customer_Address => {:street java.lang.String, :zip java.lang.Long, :country {:name java.lang.String, :code java.lang.Long}}
;; Customer_Address_Country => {:name java.lang.String, :code java.lang.Long}
```

## 0.7.0 (26.2.2014)

- support for `s/maybe` and `s/both`
- consume `Date` & `DateTime` both with and without millis: `"2014-02-18T18:25:37.456Z"` & `"2014-02-18T18:25:37Z"`
- updated docs
- more tests

## 0.6.0 (19.2.2014)

- Model, serialization and coercion support for `org.joda.time.LocalDate`
- Supports now model sequences in (body) parameters

## 0.5.0 (18.2.2014)

- Model, serialization and coercion support for `java.util.Date` and `org.joda.time.DateTime`

## 0.4.1 (16.2.2014)

- Fixed JSON Array -> Clojure Set coercion with Strings

## 0.4.0 (13.2.2014)

* Initial public version
