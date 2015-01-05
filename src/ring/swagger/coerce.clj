(ns ring.swagger.coerce
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [ring.swagger.common :refer :all])
  (:import [org.joda.time LocalDate DateTime]
           [java.util Date]
           [java.util.regex Pattern]))

(defn date-time? [x] (#{Date DateTime} x))
(defn ->DateTime [date] (if (instance? Date date) (tc/from-date date) date))

(defn parse-date-time ^DateTime [date] (tf/parse (tf/formatters :date-time-parser) (->DateTime date)))
(defn parse-date ^DateTime [date] (tf/parse-local-date (tf/formatters :date) (->DateTime date)))

(defn unparse-date-time ^String [date] (tf/unparse (tf/formatters :date-time) (->DateTime date)))
(defn unparse-date ^String [date] (tf/unparse-local-date (tf/formatters :date) (->DateTime date)))

(defn parse-pattern ^Pattern [pattern] (re-pattern pattern))
(defn unparse-pattern ^String [pattern] (.toString pattern))

(defn date-time-matcher
  [schema]
  (if (date-time? schema)
    (fn [x]
      (if (string? x)
        (let [parsed (parse-date-time x)]
          (if (= schema Date) (.toDate parsed) parsed))
        x))))

(defn date-matcher
  [schema]
  (if (= LocalDate schema)
    (fn [x]
      (if (string? x)
        (parse-date x)
        x))))

(defn pattern-matcher
  [schema]
  (if (= Pattern schema)
    (fn [x]
      (if (string? x)
        (parse-pattern x)
        x))))

(defn set-matcher
  [schema]
  (if (instance? clojure.lang.APersistentSet schema)
    (fn [x] (if (sequential? x) (set x) x))))

(defn string->boolean [x]
  (condp = x
    "true" true
    "false" false
    x))

(defn string->long [^String x]
  (try (java.lang.Long/valueOf x) (catch Exception e x)))

(defn string->double [^String x]
  (try (java.lang.Double/valueOf x) (catch Exception e x)))

(defn string->uuid [^String x]
  (try (java.util.UUID/fromString x) (catch Exception e x)))

(def json-coersions {s/Keyword sc/string->keyword
                     clojure.lang.Keyword sc/string->keyword
                     s/Int sc/safe-long-cast
                     Long sc/safe-long-cast
                     Double double
                     s/Uuid string->uuid})

(def query-coercions {s/Int string->long
                      Long string->long
                      Double string->double
                      Boolean string->boolean
                      s/Uuid string->uuid})

(defn json-schema-coercion-matcher
  [schema]
  (or (json-coersions schema)
      (sc/keyword-enum-matcher schema)
      (set-matcher schema)
      (date-time-matcher schema)
      (date-matcher schema)
      (pattern-matcher schema)))

(defn query-schema-coercion-matcher
  [schema]
  (or (query-coercions schema)
      (json-schema-coercion-matcher schema)))

(defn all-matchers
  "A matcher that applies all matchers"
  [matchers]
  (fn [schema] (reduce (fn [schema matcher]
                         (or (matcher schema) schema)) schema matchers)))

;;
;; Public Api
;;

(defn coercer [name]
  (condp = name
    :json  json-schema-coercion-matcher
    ;;:both  (all-matchers [query-schema-coercion-matcher json-schema-coercion-matcher])
    :query query-schema-coercion-matcher))
