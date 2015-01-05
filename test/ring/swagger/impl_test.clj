(ns ring.swagger.impl-test
  (:require [midje.sweet :refer :all]
            [ring.swagger.impl :refer :all]
            [schema.core :as s]))

(fact "scrict-schema strips open keys"
  (strict-schema {s/Keyword s/Any :s String}) => {:s String})

(fact "loose-schema adds open keys to top-level"
  (loose-schema {:s String}) => {s/Keyword s/Any :s String})

