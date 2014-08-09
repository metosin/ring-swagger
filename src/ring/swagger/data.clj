(ns ring.swagger.data
  (:require [schema.core :as s])
  (:import [java.util Date UUID]
           [org.joda.time DateTime LocalDate]))

;;
;; These are just internal mappings from Classes to predicates
;; don't use in client code.
;;

(def Long*     s/Int)
(def Double*   (s/pred (partial instance? Double) 'double?))
(def String*   (s/pred string? 'string?))
(def Boolean*  (s/pred (partial instance? Boolean) 'boolean?))
(def Keyword*  s/Keyword)
(def DateTime* (s/pred (fn [x] (#{Date DateTime} (class x))) 'date-time?))
(def Date*     (s/pred (fn [x] (#{LocalDate} (class x))) 'date?))
(def UUID*     (s/pred (partial instance? UUID) 'uuid?))
