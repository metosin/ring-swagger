(ns ring.swagger.coerce
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [schema.macros :as sm]
            [schema.utils :as su]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [slingshot.slingshot :refer [throw+]]
            [ring.swagger.common :refer :all]
            [ring.swagger.data :refer :all])
  (:import [org.joda.time LocalDate DateTime]
           [java.util Date]))

(def date-formatter (tf/formatters :date))
(def date-time-formatter (tf/formatters :date-time-no-ms))

(defn set-matcher
  [schema]
  (if (instance? clojure.lang.PersistentHashSet schema)
    (fn [x]
      (if (string? x)
        (set [x])
        (set x)))))

(defn date-time-matcher
  [schema]
  (if (date-time? schema)
    (fn [x]
      (if (string? x)
        (let [parsed (tf/parse date-time-formatter x)]
          (if (= schema Date) (.toDate parsed) parsed))
        x))))

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
