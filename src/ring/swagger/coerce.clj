(ns ring.swagger.coerce
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [clojure.string :as string]
            [ring.swagger.json-schema :as js]
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
  (if (instance? APersistentSet schema)
    (fn [x] (if (sequential? x) (set x) x))))

(defn string->boolean [x]
  (condp = x
    "true" true
    "false" false
    x))

(defn string->long [^String x]
  (try (Long/valueOf x) (catch Exception e x)))

(defn string->double [^String x]
  (try (Double/valueOf x) (catch Exception e x)))

(defn string->uuid [^String x]
  (try (UUID/fromString x) (catch Exception e x)))

(def json-coersions {s/Keyword sc/string->keyword
                     Keyword sc/string->keyword
                     s/Int sc/safe-long-cast
                     Long sc/safe-long-cast
                     Double double
                     s/Uuid string->uuid})

(def query-coercions {s/Int string->long
                      Long string->long
                      Double string->double
                      Boolean string->boolean
                      s/Uuid string->uuid})

;;
;; Query params
;;

(defrecord SplitParams [type split coll]
  schema.core.Schema
  (walker [this]
    (let [sub-walker (s/subschema-walker coll)]
      (fn [x]
        (if (schema.utils/error? x)
          x
          (sub-walker x)))))
  (explain [this] (cons 'params coll)))

(defmethod js/json-type SplitParams [x]
  (merge {:collectionFormat (:type x)}
         (js/json-type (:coll x))))

(defn split-params-matcher [schema]
  (if (instance? SplitParams schema)
    (fn [x]
      (if (string? x)
        (string/split x (:split schema))
        x))))

(defrecord MultiParams [coll]
  schema.core.Schema
  (walker [this]
    (let [sub-walker (s/subschema-walker coll)]
      (fn [x]
        (if (schema.utils/error? x)
          x
          (sub-walker x)))))
  (explain [this] (cons 'params coll)))

(defmethod js/json-type MultiParams [x]
  (merge {:collectionFormat "multi"}
         (js/json-type (:coll x))))

(defn multi-params-matcher
  "If only one parameter is provided to multi param, ring
   doesn't wrap the param is collection."
  [schema]
  ; Default
  (if (or (and (coll? schema) (not (record? schema))) (instance? MultiParams schema))
    (fn [x]
      (if-not (coll? x)
        [x]
        x))))

(defn csv-params [coll] (->SplitParams "csv" #"," coll))
(defn ssv-params [coll] (->SplitParams "ssv" #" " coll))
(defn tsv-params [coll] (->SplitParams "tsv" #"\\t" coll))
(defn pipe-params [coll] (->SplitParams "pipes" #"\|" coll))
(defn multi-params [coll] (->MultiParams coll))

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
      (multi-params-matcher schema)
      (split-params-matcher schema)
      (json-schema-coercion-matcher schema)))

;;
;; Public Api
;;

(defmulti coercer identity)

(defmethod coercer :json    [_] json-schema-coercion-matcher)
(defmethod coercer :query   [_] query-schema-coercion-matcher)
(defmethod coercer :default [c] c)

