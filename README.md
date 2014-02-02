# Ring-Swagger

[Swagger](...) implementation for Ring using Prismatic [Schema](https://github.com/Prismatic/schema) for data modelling.

- Provides handlers for both [Resource listing](https://github.com/wordnik/swagger-core/wiki/Resource-Listing) and [Api declaration](https://github.com/wordnik/swagger-core/wiki/API-Declaration).
- Does not cover how the routes, models and endpoints are gathered from your web app (and by so should be compatible and a base implementation for all Ring-based web frameworks)
- Provides a clean Map-based interface to create Swagger Spec out of the route definitions

For embedding a [Swagger-UI](https://github.com/wordnik/swagger-ui) into your Ring-app, check out the [Ring-swagger-ui](https://github.com/metosin/ring-swagger-ui).

## Existing Adapters
- [Compojure-Api](https://github.com/metosin/compojure-api) for Compojure

Want to integrate your favourite web framework to use this lib? Check out the [Tests](https://github.com/metosin/ring-swagger/blob/master/test/ring/swagger/core_test.clj#L116-L214).

## TODO
- [ ] error messages
- [ ] consumes
- [ ] travis

## License

Copyright © 2014 Metosin Oy

Distributed under the Eclipse Public License, the same as Clojure.
