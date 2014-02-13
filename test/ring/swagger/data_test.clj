(ns ring.swagger.data-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.data :refer :all]))

(fact "enum?"
  (enum? s/Int) => false
  (enum? (s/enum [:a :b])) => true)
