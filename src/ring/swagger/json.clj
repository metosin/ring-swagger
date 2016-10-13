(ns ring.swagger.json
  (:require [cheshire.generate :refer [add-encoder]]
            [schema.utils :as su]
            [ring.swagger.coerce :as coerce]
            [ring.swagger.extension :as extension])
  (:import [com.fasterxml.jackson.core JsonGenerator]
           [schema.utils ValidationError]
           [java.util Date]
           [java.util.regex Pattern]
           [org.joda.time DateTime LocalDate LocalTime]))

(defn date-time-encoder [x ^JsonGenerator jg]
  (.writeString jg (coerce/unparse-date-time x)))

;;
;; JSON Encoders
;;

(add-encoder ValidationError
  (fn [x ^JsonGenerator jg]
    (.writeString jg (str (su/validation-error-explain x)))))

(add-encoder Date date-time-encoder)

(add-encoder DateTime date-time-encoder)

(add-encoder LocalDate
  (fn [x ^JsonGenerator jg]
    (.writeString jg (coerce/unparse-date x))))

(add-encoder LocalTime
  (fn [x ^JsonGenerator jg]
    (.writeString jg (coerce/unparse-time x))))

(add-encoder Pattern
  (fn [x ^JsonGenerator jg]
    (.writeString jg (coerce/unparse-pattern x))))

(extension/java-time
  (add-encoder java.time.Instant
    (fn [^java.time.Instant x ^JsonGenerator jg]
      (.writeString jg (.toString x))))

  (add-encoder java.time.LocalDate
    (fn [^java.time.LocalDate x ^JsonGenerator jg]
      (.writeString jg (.toString x))))

  (add-encoder java.time.LocalTime
    (fn [^java.time.LocalTime x ^JsonGenerator jg]
      (.writeString jg (.toString x)))))
