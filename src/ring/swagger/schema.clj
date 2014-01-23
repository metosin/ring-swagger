(ns ring.swagger.schema
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [schema.macros :as sm]
            [ring.swagger.common :refer :all]))

(defn field [pred metadata]
  (with-meta pred (merge (meta pred) metadata)))

(defn required [k] (s/required-key k))
(defn optional [k] (s/optional-key k))

(defmacro defmodel [model form]
  `(def ~model ~(str model) (with-meta ~form {:model (var ~model)})))

(defn schema-name [x] (-> x meta :model name-of))

(defn coerce [model] (sc/coercer (value-of model) sc/json-coercion-matcher))
