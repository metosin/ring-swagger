(ns ring.swagger.data
  (:require [schema.core :as s])
  (:import [java.util Date]
           [org.joda.time DateTime]))

(defn date-time? [x]
  (#{Date DateTime} x))

(def Long*     s/Int)
(def Double*   (s/pred (partial instance? Double) 'double?))
(def String*   (s/pred string? 'string?))
(def Boolean*  (s/pred (partial instance? Boolean) 'boolean?))
(def Keyword*  s/Keyword)
(def DateTime* (s/pred (fn [x] (#{Date DateTime} (class x))) 'date-time?))

(defn enum? [x] (= (class x) schema.core.EnumSchema))
