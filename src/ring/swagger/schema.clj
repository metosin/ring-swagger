(ns ring.swagger.schema
  (:require [schema.core :as s]
            [schema.coerce :as sc]
            [schema.macros :as sm]
            [schema.utils :as su]
            [slingshot.slingshot :refer [throw+]]
            [ring.swagger.common :refer :all]
            [ring.swagger.data :refer :all]))

(def Keyword  s/Keyword)

(def type-map
  {Long     Long*
   Double   Double*
   String   String*
   Boolean  Boolean*
   Keyword  Keyword*})

(let [set-matcher (fn [schema] (when (instance? clojure.lang.PersistentHashSet schema) (fn [x] (if (string? x) (set [x]) (set x)))))
      coercions {s/Keyword sc/string->keyword
                 clojure.lang.Keyword sc/string->keyword
                 s/Int sc/safe-long-cast
                 Long sc/safe-long-cast
                 Double double}]
  (defn json-coercion-matcher
    "A matcher that coerces keywords and keyword enums from strings, and longs and doubles
     from numbers on the JVM (without losing precision)"
    [schema]
    (or (coercions schema)
        (sc/keyword-enum-matcher schema)
        (set-matcher schema))))

;;
;; Public Api
;;

(defn field [pred metadata]
  (let [pred (or (type-map pred) pred)
        old-meta (meta pred)]
    (with-meta pred (merge old-meta metadata))))

(defn coerce [model value]
  ((sc/coercer (value-of model) json-coercion-matcher) value))

(defn coerce! [model value]
  (let [result (coerce model value)]
    (if (su/error? result)
      (throw+ {:type ::validation :error (:error result)})
      result)))

(defmacro defmodel [model form]
  `(def ~model ~(str model) (with-meta ~form {:model (var ~model)})))

(defn model? [x] (and (map? x) (var? (:model (meta x)))))

(defn model-of [x]
  (let [value (value-of x)]
    (if (model? value)
      (:model (meta value)))))

(defn schema-name [x] (some-> x model-of name-of))
