(ns ring.swagger.coerce-test
  (:require [midje.sweet :refer :all]
            [schema.core :as s]
            [ring.swagger.coerce :as rsc]
            [ring.swagger.extension :as extension]
            [schema.coerce :as sc])
  (:import [org.joda.time LocalDate DateTime LocalTime]
           [java.util Date UUID]))

(fact "json coercions"
  (let [cooerce (rsc/coercer :json)]

    (fact "Date with and without millis"
      ((cooerce Date) "2014-02-18T18:25:37.456Z") => (partial instance? Date)
      ((cooerce Date) "2014-02-18T18:25:37Z") => (partial instance? Date))

    (fact "DateTime with and without millis"
      ((cooerce DateTime) "2014-02-18T18:25:37.456Z") => (partial instance? DateTime)
      ((cooerce DateTime) "2014-02-18T18:25:37Z") => (partial instance? DateTime))

    (fact "LocalDate"
      ((cooerce LocalDate) "2014-02-19") => (partial instance? LocalDate))

    (fact "LocalTime"
      ((cooerce LocalTime) "10:23") => (partial instance? LocalTime)
      ((cooerce LocalTime) "10:23:37") => (partial instance? LocalTime)
      ((cooerce LocalTime) "10:23:37.456") => (partial instance? LocalTime))

    (fact "UUID"
      ((cooerce UUID) "77e70512-1337-dead-beef-0123456789ab") => (partial instance? UUID))

    (extension/java-time
      (fact "java.time"
        (fact "Instant with and without millis"
          ((cooerce java.time.Instant) "2014-02-18T18:25:37.456Z") => (partial instance? java.time.Instant)
          ((cooerce java.time.Instant) "2014-02-18T18:25:37Z") => (partial instance? java.time.Instant))

        (fact "LocalDate"
          ((cooerce java.time.LocalDate) "2014-02-19") => (partial instance? java.time.LocalDate))

        (fact "LocalTime"
          ((cooerce java.time.LocalTime) "10:23") => (partial instance? java.time.LocalTime)
          ((cooerce java.time.LocalTime) "10:23:37") => (partial instance? java.time.LocalTime)
          ((cooerce java.time.LocalTime) "10:23:37.456") => (partial instance? java.time.LocalTime))))))

(fact "query coercions"
  (let [coerce (rsc/coercer :query)]

    (fact "s/Int"
      ((coerce s/Int) 1) => (partial instance? Long)
      ((coerce s/Int) "1") => 1
      ((coerce s/Int) "1.2") => "1.2")

    (fact "Long"
      ((coerce Long) (int 1)) => (partial instance? Long)
      ((coerce Long) 1) => 1
      ((coerce Long) "1") => 1
      ((coerce Long) "1.2") => "1.2")

    (fact "Double"
      ((coerce Double) 1) => 1.0
      ((coerce Double) "1") => 1.0
      ((coerce Double) "invalid") => "invalid")

    (fact "s/Num"
      ((coerce s/Num) 1) => 1
      ((coerce s/Num) 1.0) => 1.0
      ((coerce s/Num) "1") => 1
      ((coerce s/Num) "1.0") => 1.0
      ((coerce s/Num) "-1.0") => -1.0
      ((coerce s/Num) "+1.0") => 1.0
      ((coerce s/Num) "1.0e10") => 1.0e10
      ((coerce s/Num) "invalid") => "invalid")

    (fact "Boolean"
      ((coerce Boolean) true) => true
      ((coerce Boolean) "true") => true
      ((coerce Boolean) "false") => false
      ((coerce Boolean) "invalid") => "invalid")

    (fact "UUID"
      ((coerce UUID) #uuid "77e70512-1337-dead-beef-0123456789ab") => #uuid "77e70512-1337-dead-beef-0123456789ab"
      ((coerce UUID) "77e70512-1337-dead-beef-0123456789ab") => #uuid "77e70512-1337-dead-beef-0123456789ab"
      ((coerce UUID) "invalid") => "invalid")))
