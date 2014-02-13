(ns ring.swagger.data
  (:require [schema.core :as s]))

(def Long*     s/Int)
(def Double*   (s/pred (partial instance? Double) 'double?))
(def Str*      (s/pred string? 'string?))
(def Boolean*  (s/pred (partial instance? Boolean) 'boolean?))
(def Keyword*  s/Keyword)

(defn enum? [x] (= (class x) schema.core.EnumSchema))
