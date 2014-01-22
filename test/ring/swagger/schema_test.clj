(ns ring.swagger.schema-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.schema :refer :all]))

(defmodel Inty {:int s/Int})

(fact "defmodel"
  (:model (meta Inty)) => (var Inty))
