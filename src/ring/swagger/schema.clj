(ns ring.swagger.schema
  (:require [schema.core :as s]
            [schema.macros :as sm]
            [ring.swagger.common :refer :all]))

(def sString
  "Clojure String Predicate enabling setting metadata to it."
  (s/pred string? 'string?))

(defn field [pred metadata]
  (let [pred (if (= s/String pred) sString pred)
        old-meta (meta pred)]
    (with-meta pred (merge old-meta metadata))))

(defn required [k] (s/required-key k))
(defn optional [k] (s/optional-key k))

(defmacro defmodel [model form]
  `(def ~model ~(str model) (with-meta ~form {:model (var ~model)})))

(defn schema-name [x] (-> x meta :model name-of))
