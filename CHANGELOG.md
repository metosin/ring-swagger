## 0.8.6 (9.4.2014)

- new `ring.swagger.ui`-ns provided `swagger-ui` function which can be used to create ring handler to serve Swagger-ui.
- updated clojure dependency

## 0.8.5 (7.4.2014)

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

- support for `schema/maybe` and `schema/both`
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
