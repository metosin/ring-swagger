(ns ring.swagger.coerce-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.coerce :refer :all])
  (:import [org.joda.time LocalDate DateTime]
           [java.util Date UUID]))

(fact "json coercions"
  (let [c (coercer :json)]

    (fact "Date with / without millis and and from long / string-long"
      ((c Date) "2014-02-18T18:25:37.456Z") => (partial instance? Date)
      ((c Date) "2014-02-18T18:25:37Z") => (partial instance? Date)
      ((c Date) "1432439999999") => (partial instance? Date)
      ((c Date) 1432439999999) => (partial instance? Date))

    (fact "DateTime with / without millis and from long / string-long"
      ((c DateTime) "2014-02-18T18:25:37.456Z") => (partial instance? DateTime)
      ((c DateTime) "2014-02-18T18:25:37Z") => (partial instance? DateTime)
      ((c DateTime) "1432439999999") => (partial instance? DateTime)
      ((c DateTime) 1432439999999) => (partial instance? DateTime))

    (fact "LocalDate from string, long / string-long"
      ((c LocalDate) "2014-02-19") => (partial instance? LocalDate)
      ((c LocalDate) "1432439999999") => (partial instance? LocalDate)
      ((c LocalDate) 1432439999999) => (partial instance? LocalDate))

    (fact "UUID"
      ((c UUID) "77e70512-1337-dead-beef-0123456789ab") => (partial instance? UUID))))

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
      ((c Boolean) "invalid") => "invalid")

    (fact "UUID"
      ((c UUID) "77e70512-1337-dead-beef-0123456789ab") => (UUID/fromString "77e70512-1337-dead-beef-0123456789ab")
      ((c UUID) "invalid") => "invalid")))
