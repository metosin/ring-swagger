(ns ring.swagger.coerce-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.coerce :refer :all]))

(fact "query coercions"
  (let [c (coercer :query)]

    (fact "s/Int"
      ((c s/Int) "1") => 1
      ((c s/Int) "1.2") => "1.2")

    (fact "Long"
      ((c Long) "1") => 1
      ((c Long) "1.2") => "1.2")

    (fact "Double"
      ((c Double) "1") => 1.0
      ((c Double) "invalid") => "invalid")

    (fact "Boolean"
      ((c Boolean) "true") => true
      ((c Boolean) "false") => false
      ((c Boolean) "invalid") => "invalid")))
