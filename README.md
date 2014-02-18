# Ring-Swagger

[![Build Status](https://travis-ci.org/metosin/ring-swagger.png?branch=master)](https://travis-ci.org/metosin/ring-swagger)

[Swagger](...) implementation for Ring using Prismatic [Schema](https://github.com/Prismatic/schema) for data modelling and input data coercion.

- Provides handlers for both [Resource listing](https://github.com/wordnik/swagger-core/wiki/Resource-Listing) and [Api declaration](https://github.com/wordnik/swagger-core/wiki/API-Declaration).
- Does not cover how the routes and models are collected from web apps (and by so should be compatible with all Ring-based libraries)
   - Provides a Map-based interface for routing libs to create Swagger Spec out of the route definitions

For embedding a [Swagger-UI](https://github.com/wordnik/swagger-ui) into your Ring-app, check out the [ring-swagger-ui](https://github.com/metosin/ring-swagger-ui).

## Installation

    [metosin/ring-swagger "0.5.0"]

## Existing Adapters
- [Compojure-Api](https://github.com/metosin/compojure-api) for Compojure

## Writing new Adapter
Check out the [Tests](https://github.com/metosin/ring-swagger/blob/master/test/ring/swagger/core_test.clj#L116-L214).

## Supported Schema elements

| Element | JSON  |
| --------|:------------:|
| `Long`, `schema/Int` | integer, int64
| `Double` | number, double
| `String`, `schema/str` | string
| Keyword, `schema/Keyword` | string
| `Boolean` | boolean
| `java.util.Date`, `org.joda.time.DateTime` | string, date-time

- Supports also `schema/enum`s, Vectors, Sets, Maps (Complex Types) and References. References are resolved automatically.
- Has a tuned *Schema coercion* for transforming the input data into vanilla Clojure, supporting the following coercions:
  - numbers -> `Long` or `Double`
  - string -> keyword
  - vectors -> sets
- `Integer`, `Byte` and `Float` are not supported as they can be handled as `Long`s and `Double`s.

see [Tests](https://github.com/metosin/ring-swagger/blob/master/test/ring/swagger/schema_test.clj).

## TODO

- support for `LocalDate`
- support for consumes
- non-json produces & consumes

## License

Copyright © 2014 Metosin Oy

Distributed under the Eclipse Public License, the same as Clojure.
