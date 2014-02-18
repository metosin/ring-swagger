(ns ring.swagger.coerce
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [ring.swagger.common :refer :all]
            [ring.swagger.data :refer :all])
  (:import [org.joda.time LocalDate DateTime]
           [java.util Date]))


(def date-formatter (tf/formatters :date))
(def date-time-formatter (tf/formatters :date-time))

(defn ->DateTime [date] (if (instance? Date date) (tc/from-date date) date))

(defn unparse [fmt date] (tf/unparse fmt (->DateTime date)))
(defn parse [fmt date] (tf/parse fmt (->DateTime date)))

(defn date-time-matcher
  [schema]
  (if (date-time? schema)
    (fn [x]
      (if (string? x)
        (let [parsed (parse date-time-formatter x)]
          (if (= schema Date) (.toDate parsed) parsed))
        x))))

(defn set-matcher
  [schema]
  (if (instance? clojure.lang.PersistentHashSet schema)
    (fn [x]
      (if (string? x)
        (set [x])
        (set x)))))

(def coercions {s/Keyword sc/string->keyword
                clojure.lang.Keyword sc/string->keyword
                s/Int sc/safe-long-cast
                Long sc/safe-long-cast
                Double double})

;;
;; Public Api
;;

(defn json-schema-coercion-matcher
  [schema]
  (or (coercions schema)
    (sc/keyword-enum-matcher schema)
    (set-matcher schema)
    (date-time-matcher schema)))
