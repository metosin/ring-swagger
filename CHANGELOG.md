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
