(ns ring.swagger.schema
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [schema.macros :as sm]
            [ring.swagger.common :refer :all]))

(def Str*
  "Clojure String Predicate enabling setting metadata to it."
  (s/pred string? 'string?))

(defn field [pred metadata]
  (let [pred (if (= s/Str pred) Str* pred)
        old-meta (meta pred)]
    (with-meta pred (merge old-meta metadata))))

(defmacro defmodel [model form]
  `(def ~model ~(str model) (with-meta ~form {:model (var ~model)})))

(defn schema-name [x] (-> x value-of meta :model name-of))

(defn coerce [model] (sc/coercer (value-of model) sc/json-coercion-matcher))
