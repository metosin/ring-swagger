# Ring-Swagger

[![Build Status](https://travis-ci.org/metosin/ring-swagger.png?branch=master)](https://travis-ci.org/metosin/ring-swagger)

[Swagger](...) implementation for Ring using Prismatic [Schema](https://github.com/Prismatic/schema) for data modelling and input data coercion.

- Provides handlers for both [Resource listing](https://github.com/wordnik/swagger-core/wiki/Resource-Listing) and [Api declaration](https://github.com/wordnik/swagger-core/wiki/API-Declaration).
- Does not cover how the routes and models are collected from web apps (and by so should be compatible with all Ring-based libraries)
   - Provides a Map-based interface for routing libs to create Swagger Spec out of the route definitions

For embedding a [Swagger-UI](https://github.com/wordnik/swagger-ui) into your Ring-app, check out the [ring-swagger-ui](https://github.com/metosin/ring-swagger-ui).

## Latest version

[![Latest version](https://clojars.org/metosin/ring-swagger/latest-version.svg)](https://clojars.org/metosin/ring-swagger)

## Client libraries

- [Compojure-Api](https://github.com/metosin/compojure-api) for Compojure

If your favourite web lib doesn't have an client adapter, you should write an it yourself. Here's howto:

1. Create routes for `api-docs` and `api-docs/:api`
2. Create route-collector to [collect the routes](https://github.com/metosin/ring-swagger/blob/master/test/ring/swagger/core_test.clj).
3. Test it.
4. Ship it.
5. Profit.

## Models

The building blocks for creating Web Schemas are found in package `ring.swagger.schema`. All schemas must be declared by `defmodel`, which set up the needed meta-data. Otherwise, it's just a normal [Schema](https://github.com/Prismatic/schema).

### Supported Schema elements

| Clojure | JSON Schema | Sample  |
| --------|-------|:------------:|
| `Long`, `schema/Int`        | integer, int64 | `1`|
| `Double`                    | number, double | `1.2`
| `String`, `schema/Str`, Keyword, `schema/Keyword`      | string | `"kikka"`
| `Boolean`                   | boolean | `true`
| `java.util.Date`, `org.joda.time.DateTime`  | string, date-time | `"2014-02-18T18:25:37.456Z"`
| `org.joda.time.LocalDate`   | string, date | `"2014-02-19"`
| `(schema/enum X Y Z)`       | string enum(X,Y,Z)
| `(schema/maybe X)`          | *type of X*
| `(schema/both X Y Z)`       | *type of X*

- Vectors, Sets and Maps can be used as containers
  - Maps are presented as Complex Types and References. Model references are resolved automatically.
  - Nested maps are not supported (by the Spec), use references instead.
- Utilizes [Schema coercions](http://blog.getprismatic.com/blog/2014/1/4/schema-020-back-with-clojurescript-data-coercion) for transforming the input data into vanilla Clojure and back, supporting the following:
  - numbers -> `Long` or `Double`
  - string -> Keyword
  - string -> `java.util.Date`, `org.joda.time.DateTime` or `org.joda.time.LocalDate`
  - vectors -> Sets
- `Integer`, `Byte` and `Float` are not supported as they can be handled more idiomatic as `Long`s and `Double`s.

### A Sample Schema

```clojure
(require '[ring.swagger.schema :refer :all])
(require '[schema.core :as s])

(defmodel SubType  {:alive Boolean})
(defmodel AllTypes {:a Boolean
                    :b Double
                    :c Long
                    :d String
                    :e {:f [Keyword]
                        :g #{String}
                        :h #{(s/enum :kikka :kakka :kukka)}
                        :i Date
                        :j DateTime
                        :k LocalDate
                        :l (s/maybe String)
                        :m (s/both Long (s/pred odd? 'odd?))
                        :n SubType}})
```

see models and coercion in action in [tests](https://github.com/metosin/ring-swagger/blob/master/test/ring/swagger/schema_test.clj).

## TODO

- enum -> allowed values
- support for consumes
- non-json produces & consumes
- better support for `Schema`-predicates (`maybe`, `either`, `both`, ...)

## License

Copyright © 2014 Metosin Oy

Distributed under the Eclipse Public License, the same as Clojure.
