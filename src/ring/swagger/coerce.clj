(ns ring.swagger.coerce
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [clojure.string :as str]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [clojure.edn :as edn]
            [ring.swagger.extension :as extension])
  (:import [org.joda.time LocalDate DateTime LocalTime]
           [java.util Date UUID]
           [java.util.regex Pattern]
           (clojure.lang APersistentSet Keyword)))

(defn ->DateTime [date] (if (instance? Date date) (tc/from-date date) date))

(defn parse-date-time ^DateTime [date] (tf/parse (tf/formatters :date-time-parser) (->DateTime date)))
(defn parse-date ^DateTime [date] (tf/parse-local-date (tf/formatters :date) (->DateTime date)))
(defn parse-time ^DateTime [date] (tf/parse-local-time (tf/formatters :time-parser) (->DateTime date)))

(defn unparse-date-time ^String [date] (tf/unparse (tf/formatters :date-time) (->DateTime date)))
(defn unparse-date ^String [date] (tf/unparse-local-date (tf/formatters :date) (->DateTime date)))
(defn unparse-time ^String [date] (tf/unparse-local-time (tf/formatters :time) (->DateTime date)))

(defn parse-pattern ^Pattern [pattern] (re-pattern pattern))
(defn unparse-pattern ^String [pattern] (str pattern))

(declare custom-matcher)

(defmulti time-matcher (if (= "true" (System/getProperty "ring.swagger.coerce.identity-time-matcher-dispatch"))
                         identity
                         #(when (class? %) %)))

(defn coerce-if-string [f] (fn [x] (if (string? x) (f x) x)))

(defmethod time-matcher Date      [_] (coerce-if-string (fn [x] (.toDate (parse-date-time x)))))
(defmethod time-matcher DateTime  [_] (coerce-if-string parse-date-time))
(defmethod time-matcher LocalDate [_] (coerce-if-string parse-date))
(defmethod time-matcher LocalTime [_] (coerce-if-string parse-time))

(extension/java-time
  (defmethod time-matcher java.time.Instant [_]
    (coerce-if-string (fn [x] (java.time.Instant/parse x))))

  (defmethod time-matcher java.time.LocalDate [_]
    (coerce-if-string (fn [x] (java.time.LocalDate/parse x))))

  (defmethod time-matcher java.time.LocalTime [_]
    (coerce-if-string (fn [x] (java.time.LocalTime/parse x)))))

(defmethod time-matcher :default [_] nil)

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

(defn string->number [^String x]
  (if (string? x)
    (try
      (let [parsed (edn/read-string x)]
        (if (number? parsed) parsed x))
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
                      s/Num (cond-matcher
                             string? string->number
                             number? identity)
                      Boolean string->boolean})

(defn json-schema-coercion-matcher
  [schema]
  (or (json-coersions schema)
      (sc/keyword-enum-matcher schema)
      (set-matcher schema)
      (time-matcher schema)
      (pattern-matcher schema)
      (custom-matcher schema)))

(defn split-params-matcher [schema]
  (if (or (and (coll? schema) (not (record? schema))))
    (fn [x]
      (if (string? x)
        (str/split x #",")
        x))))

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
      ; (split-params-matcher schema)
      (multi-params-matcher schema)
      (json-schema-coercion-matcher schema)))

;;
;; Public Api
;;

(defmulti custom-matcher (if (= "true" (System/getProperty "ring.swagger.coerce.identity-custom-matcher-dispatch"))
                           identity
                           #(when (class? %) %)))
(defmethod custom-matcher :default [_] nil)

(defmulti coercer (if (= "true" (System/getProperty "ring.swagger.coerce.identity-coercer-dispatch"))
                    identity
                    #(when (keyword? %) %)))

(defmethod coercer :json [_] json-schema-coercion-matcher)
(defmethod coercer :query [_] query-schema-coercion-matcher)
(defmethod coercer :default [c] c)
