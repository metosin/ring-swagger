(ns ring.swagger.coerce-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.coerce :refer :all])
  (:import [org.joda.time LocalDate DateTime]
    [java.util Date]))

(fact "json coercions"
  (let [c (coercer :json)]

    (fact "Date with and without millis"
      ((c Date) "2014-02-18T18:25:37.456Z") => (partial instance? Date)
      ((c Date) "2014-02-18T18:25:37Z") => (partial instance? Date))

    (fact "DateTime with and without millis"
      ((c DateTime) "2014-02-18T18:25:37.456Z") => (partial instance? DateTime)
      ((c DateTime) "2014-02-18T18:25:37Z") => (partial instance? DateTime))

    (fact "LocalDate"
      ((c LocalDate) "2014-02-19") => (partial instance? LocalDate))))

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
