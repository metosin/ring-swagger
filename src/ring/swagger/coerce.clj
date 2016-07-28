(ns ring.swagger.coerce
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [clojure.string :as string]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [ring.swagger.common :refer :all])
  (:import [org.joda.time LocalDate DateTime]
           [java.util Date UUID]
           [java.util.regex Pattern]
           (clojure.lang APersistentSet Keyword)))

(defn date-time? [x] (#{Date DateTime} x))
(defn ->DateTime [date] (if (instance? Date date) (tc/from-date date) date))

(defn parse-date-time ^DateTime [date] (tf/parse (tf/formatters :date-time-parser) (->DateTime date)))
(defn parse-date ^DateTime [date] (tf/parse-local-date (tf/formatters :date) (->DateTime date)))

(defn unparse-date-time ^String [date] (tf/unparse (tf/formatters :date-time) (->DateTime date)))
(defn unparse-date ^String [date] (tf/unparse-local-date (tf/formatters :date) (->DateTime date)))

(defn parse-pattern ^Pattern [pattern] (re-pattern pattern))
(defn unparse-pattern ^String [pattern] (str pattern))

(defn date-time-matcher [schema]
  (if (date-time? schema)
    (fn [x]
      (if (string? x)
        (let [parsed (parse-date-time x)]
          (if (= schema Date) (.toDate parsed) parsed))
        x))))

(defn date-matcher [schema]
  (if (= LocalDate schema)
    (fn [x]
      (if (string? x)
        (parse-date x)
        x))))

(defn pattern-matcher [schema]
  (if (= Pattern schema)
    (fn [x]
      (if (string? x)
        (parse-pattern x)
        x))))

(defn set-matcher [schema]
  (if (instance? APersistentSet schema)
    (fn [x]
      (if (sequential? x)
        (set x)
        x))))

(defn string->boolean [x]
  (if (string? x)
    (condp = x
      "true" true
      "false" false
      x)
    x))

(string->boolean true)

(defn string->long [^String x]
  (if (string? x)
    (try
      (Long/valueOf x)
      (catch Exception e
        x))
    x))

(defn string->double [^String x]
  (if (string? x)
    (try
      (Double/valueOf x)
      (catch Exception _ x))
    x))

(defn string->uuid [^String x]
  (if (string? x)
    (try
      (UUID/fromString x)
      (catch Exception _ x))
    x))

(defn number->double [^Number x]
  (if (number? x)
    (try
      (double x)
      (catch Exception _ x))
    x))

(defmacro cond-matcher [& conds]
  (let [x (gensym "x")]
    `(fn [~x]
       (cond
         ~@(for [c conds] `(~c ~x))
         :else ~x))))

(def json-coersions {s/Keyword sc/string->keyword
                     Keyword sc/string->keyword
                     s/Uuid string->uuid
                     s/Int sc/safe-long-cast
                     Long sc/safe-long-cast
                     Double double})

(def query-coercions {s/Keyword sc/string->keyword
                      Keyword sc/string->keyword
                      s/Uuid string->uuid
                      s/Int (cond-matcher
                              string? string->long
                              number? sc/safe-long-cast)
                      Long (cond-matcher
                             string? string->long
                             number? sc/safe-long-cast)
                      Double (cond-matcher
                               string? string->double
                               number? number->double)
                      Boolean string->boolean})

(defn json-schema-coercion-matcher
  [schema]
  (or (json-coersions schema)
      (sc/keyword-enum-matcher schema)
      (set-matcher schema)
      (date-time-matcher schema)
      (date-matcher schema)
      (pattern-matcher schema)))

(def collection-format-split-regex
  {"csv" #","
   "ssv" #" "
   "tsv" #"\t"
   "pipes" #"\|"})

(defn split-params-matcher [schema]
  (if (or (and (coll? schema) (not (record? schema))))
    ;; FIXME: Can't use json-schema/json-schema-meta because of cyclic dependency
    (let [collection-format (:collectionFormat (:json-schema (meta schema)) "csv")
          split-regex (get collection-format-split-regex collection-format)]
      (if split-regex
        (fn [x]
          (if (string? x)
            (string/split x split-regex)
            x))))))

(defn multi-params-matcher
  "If only one parameter is provided to multi param, ring
   doesn't wrap the param is collection."
  [schema]
  ; Default
  (if (or (and (coll? schema) (not (record? schema))))
    (fn [x]
      (if-not (coll? x)
        [x]
        x))))

(defn query-schema-coercion-matcher
  [schema]
  (or (query-coercions schema)
      (split-params-matcher schema)
      (multi-params-matcher schema)
      (json-schema-coercion-matcher schema)))

;;
;; Public Api
;;

(defmulti coercer identity)

(defmethod coercer :json [_] json-schema-coercion-matcher)
(defmethod coercer :query [_] query-schema-coercion-matcher)
(defmethod coercer :default [c] c)
