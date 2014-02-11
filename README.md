# Ring-Swagger

[![Build Status](https://travis-ci.org/metosin/ring-swagger.png?branch=master)](https://travis-ci.org/metosin/ring-swagger)

[Swagger](...) implementation for Ring using Prismatic [Schema](https://github.com/Prismatic/schema) for data modelling.

- Provides handlers for both [Resource listing](https://github.com/wordnik/swagger-core/wiki/Resource-Listing) and [Api declaration](https://github.com/wordnik/swagger-core/wiki/API-Declaration).
- Does not solve how the routes, models and endpoints are collected from your web app (and by so should be compatible with all Ring-based libraries)
- Provides a clean Map-based interface to create Swagger Spec out of the route definitions

For embedding a [Swagger-UI](https://github.com/wordnik/swagger-ui) into your Ring-app, check out the [ring-swagger-ui](https://github.com/metosin/ring-swagger-ui).

## Installation

    [metosin/ring-swagger "0.2.0"]

## Existing Adapters
- [Compojure-Api](https://github.com/metosin/compojure-api) for Compojure

## Writing new Adapter
Check out the [Tests](https://github.com/metosin/ring-swagger/blob/master/test/ring/swagger/core_test.clj#L116-L214).

## Supported Schema elements

| Element | JSON-schema  |
| --------|:------------:|
| `Integer` | integer, int32
| `Long`, `schema/Int` | integer, int64
| `Float` | number, float
| `Double` | number, double
| `String`, `schema/str` | string
| `Byte` | string, byte
| `Boolean` | boolean
| `java.util.Date` | string, date
| `org.joda.time.DateTime` | string, date-time

- Arrays (not yet sets)
- Complex Types (no nesting), including references

## TODO

- support for consumes
- support for sets
- remove non-idiomatic types (Integer, Byte, Float)?
- non-json produces & consumes

## License

Copyright © 2014 Metosin Oy

Distributed under the Eclipse Public License, the same as Clojure.
